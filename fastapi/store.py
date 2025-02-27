# 업로드된 PDF 파일을 임베딩하여 ChromaDB에 저장
import os
from langchain_community.document_loaders import PyMuPDFLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_chroma import Chroma
from chromadb import PersistentClient
import fitz
from langchain.schema import Document
from chromadb.config import Settings
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# FastAPI 인스턴스 생성
app = FastAPI()

# CORSMiddleware는 Cross-Origin Resource Sharing (CORS) 를 설정하는 미들웨어로, 
# 웹 브라우저에서 다른 도메인 간 요청을 허용할지 여부를 결정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 모든 도메인 허용 (필요 시 특정 도메인만 허용)
    allow_credentials=True, # 인증 정보를 포함한 요청(예: 쿠키, 인증 헤더 등)을 허용
    allow_methods=["*"], # 모든 HTTP 메서드(GET, POST, PUT, DELETE 등) 허용
    allow_headers=["*"], # 모든 요청 헤더를 허용
)

# 요청 데이터 모델
class StoreRequest(BaseModel):
    pdf_directory: str
    
@app.on_event("startup")
async def startup_event():
    # 임베딩 모델을 한 번만 로드하여 app.state에 저장
    app.state.embeddings_model = get_embeddings_model()
    print("임베딩 모델이 로드되었습니다.")

# ------- 문서 등록되면 벡터 저장 -------
@app.post('/store')
async def docs_store(request: StoreRequest):
    pdf_directory = request.pdf_directory
    print("요청 경로:", pdf_directory)

    chroma_db_path = "./chroma_db"  # ChromaDB 저장 경로

    # PDF 로드 및 분할
    split_documents = load_and_split_pdf(pdf_directory)  

    # app.state에서 임베딩 모델 재사용
    embedding_model = app.state.embeddings_model

    # 임베딩 & ChromaDB 저장
    vectorstore = embed_and_store_in_chroma(split_documents, embedding_model, chroma_db_path)
    
    return {"message": "업로드 및 벡터 변환 완료!"}

# 임베딩할 모델 로드하는 함수
def get_embeddings_model(model_name='jhgan/ko-sroberta-nli', device='cpu'):
    """
    Hugging Face의 사전학습된 임베딩 모델을 로드하는 함수.
    :param model_name: 사용할 임베딩 모델 이름
    :param device: 실행할 디바이스 ('cpu' 또는 'cuda')
    :return: HuggingFaceEmbeddings 객체
    """
    embeddings_model = HuggingFaceEmbeddings(
        # 한국어 자연어 추론 최적화된  ko-sroberta 모델
        model_name=model_name,
        # CPU에서 실행되도록 설정
        model_kwargs={'device': device},
        # 임베딩을 정규화, 벡터가 같은 범위의 값을 갖도록 함. (유사도 계산시 일관성 높임)
        encode_kwargs={'normalize_embeddings': True},
    )
    return embeddings_model

# 업로드 파일을 분할하여 all_split_documents 리스트에 반환
def load_and_split_pdf(pdf_path: str, chunk_size=500, chunk_overlap=200):
    """
    지정된 PDF 파일을 로드하고, 각 페이지별로 분할하여 텍스트 청크 리스트를 반환하는 함수.
    :param pdf_path: PDF 파일 경로
    :param chunk_size: 텍스트 분할 크기
    :param chunk_overlap: 텍스트 분할 시 오버랩 크기
    :return: 분할된 텍스트 청크들의 리스트
    """
    
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        length_function=len,  # 문자열 길이를 기준으로 분할
    )
    """  
    기본적으로 문장을 끊어서 분할하려고 시도합니다.
    하지만 문장 단위로 나누기 어렵다면, 단락(줄바꿈 \n\n)을 기준으로 분할합니다.
    그래도 적절하게 나누기 어렵다면, 공백( )을 기준으로 재귀적으로 분할합니다.
    최후의 방법으로, 지정한 chunk_size 크기만큼 강제로 분할합니다. 
    """
    
    all_split_documents = []  # 모든 분할된 텍스트를 저장할 리스트

    print(f"Processing: {pdf_path}")

    # 1. PDF 로드
    doc = fitz.open(pdf_path)  # PyMuPDF로 PDF 열기
    print(f"  - Loaded {len(doc)} pages.")

    # 2. 각 페이지별로 텍스트 분할
    for page_num in range(len(doc)):
        page = doc.load_page(page_num)
        page_text = page.get_text()  # 페이지에서 텍스트 추출
        
        # 텍스트를 분할
        split_texts = text_splitter.split_text(page_text)
        
        # Document 객체 생성 (메타데이터 포함)
        for chunk in split_texts:
            all_split_documents.append(Document(
                page_content=chunk,
                metadata={"Title": pdf_path.split("/")[-1], "Page": page_num + 1}
            ))

    print(f"Total {len(all_split_documents)} text chunks from the PDF.")
    
    return all_split_documents

# 분할된 docs를 임베딩하여 ChromaDB에 저장
def embed_and_store_in_chroma(split_documents, embeddings_model, chroma_db_path="./chroma_db"):
    """
    분할된 문서를 임베딩하고 ChromaDB에 저장하는 함수.
    :param split_documents: Document 객체 리스트 (메타데이터 포함)
    :param embeddings_model: HuggingFace 임베딩 모델 객체
    :param chroma_db_path: ChromaDB가 저장될 디렉토리 경로
    :return: 생성된 Chroma 벡터스토어 객체
    """
    if not split_documents:
        print("No documents to process.")
        return None
    
    # ✅ PersistentClient를 사용하여 ChromaDB 저장
    chroma_client = PersistentClient(path=chroma_db_path)
    vectorstore = Chroma(
        client=chroma_client,
        embedding_function=embeddings_model,
        collection_metadata={"hnsw:space": "cosine"},
    )

    #✅ PersistentClient를 사용하여 디스크 기반으로 ChromaDB를 관리
    #✅ embedding_function=embeddings_model: 사전 학습된 임베딩 모델 사용
    #✅ collection_metadata={"hnsw:space": "cosine"}: 코사인 유사도 기반 검색 설정

    # 기존 데이터 삭제
    existing_docs = vectorstore.get(include=["metadatas"])
    doc_ids_to_delete = [
        doc_id for doc_id, metadata in zip(existing_docs["ids"], existing_docs["metadatas"])
        if metadata and "Title" in metadata and metadata["Title"] == split_documents[0].metadata["Title"]
    ]
    # 기존 문서의 메타데이터가 존재하고(metadata), 해당 문서에 Title 키가 있으며, 새로 추가하려는 문서의 Title과 동일한 경우에 True
    
    
    if doc_ids_to_delete:
        print(f"🗑 Deleting {len(doc_ids_to_delete)} existing vectors for {split_documents[0].metadata['Title']}")
        vectorstore.delete(doc_ids_to_delete)

    # 새로운 문서 저장
    vectorstore.add_documents(split_documents)

    print(f"✅ Successfully stored {len(split_documents)} document embeddings in ChromaDB at {chroma_db_path}.")
    return vectorstore
