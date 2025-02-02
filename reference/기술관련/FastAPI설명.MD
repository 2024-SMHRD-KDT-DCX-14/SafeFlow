#  스프링부트와 FastAPI를 별도로 실행하면서 통신

 *  스프링부트와 FastAPI를 각각 별도의 서비스로 실행하면서 통신하도록 만들 수 있습니다. 
     예를 들어, FastAPI가 특정 데이터를 처리하고 스프링부트가 이를 호출하는 방식입니다.



* 스프링부트 (Java)

  1. 프로젝트 생성: Spring Initializr를 사용해 프로젝트 생성.
  2. Dependencies: Spring Web, Spring Boot DevTools 추가.
  3. REST API 호출 코드 추가

```java
@RestController
@RequestMapping("/api")
public class ApiController {

    private final RestTemplate restTemplate;

    public ApiController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @GetMapping("/call-fastapi")
    public String callFastAPI() {
        String fastApiUrl = "http://localhost:8000/data";
        return restTemplate.getForObject(fastApiUrl, String.class);
    }
}
```

   4. RestTemplate를 사용해 FastAPI와 통신.

 * FastAPI (Python)

    1. FastAPI 설치

```bash
pip install fastapi uvicorn
```

  2. API 서버 작성

```python

from fastapi import FastAPI

app = FastAPI()

@app.get("/data")
def read_data():
    return {"message": "Hello from FastAPI"}

```

   3. 서버 실행

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```


## FastAPI를 Java 코드 안에서 실행 (Python 내장)

   - Java에서 Python 코드를 실행하려면 Jython 또는 ProcessBuilder를 사용할 수 있지만, 일반적으로 Python을 별도로 실행하는 방식이 더 효율적입니다.

   * 스프링부트 + FastAPI 통합의 핵심 포인트

      - REST API 통신: 스프링부트와 FastAPI는 REST API를 통해 쉽게 통신 가능.
      - Docker 사용: 두 애플리케이션을 각각 컨테이너로 배포해 통합 실행 가능.
      - 비동기 요청: FastAPI는 비동기 처리가 뛰어나므로, 스프링부트에서 비동기 방식으로 호출하면 성능 최적화.



### 스프링부트와 FastAPI를 사용해 간단한 REST API 통신을 구현하는 실제 예제 코드

1. FastAPI 서버 (Python)

    - FastAPI는 간단한 REST API를 제공하는 서버로 설정합니다.

    - Python 코드 (app.py)

```python
from fastapi import FastAPI

app = FastAPI()

@app.get("/data")
def get_data():
    return {"message": "Hello from FastAPI!", "status": "success"}

```

- 서버 실행

```bash

pip install fastapi uvicorn
uvicorn app:app --host 0.0.0.0 --port 8000

```

- 위 코드를 실행하면 FastAPI가 http://localhost:8000/data 경로에서 데이터를 반환.


2. 스프링부트 서버 (Java)

  - 스프링부트에서 FastAPI의 데이터를 호출합니다.


 1) Gradle 또는 Maven 설정

  * build.gradle

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-json'
}
```

   * pom.xml (Maven을 사용하는 경우)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-json</artifactId>
    </dependency>
</dependencies>

```

2) 스프링부트 컨트롤러 코드

  * ApiController.java


```java
package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ApiController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fastapi.url:http://localhost:8000/data}")
    private String fastApiUrl;

    @GetMapping("/api/fastapi")
    public String callFastAPI() {
        String response = restTemplate.getForObject(fastApiUrl, String.class);
        return "Response from FastAPI: " + response;
    }
}


```


3) application.properties 설정

  * FastAPI URL을 설정 파일에 명시할 수 있습니다.


3. 결과 테스트

1) FastAPI 실행

   * FastAPI를 먼저 실행

```bash
uvicorn app:app --host 0.0.0.0 --port 8000
```

2) 스프링부트 실행

  * 스프링부트를 실행하고, 브라우저 또는 Postman에서 다음 경로로 호출

```bash

http://localhost:8080/api/fastapi

```

3) 응답 결과

  * 스프링부트에서 FastAPI 데이터를 성공적으로 호출하면 다음과 같은 결과를 확인.

```json
Response from FastAPI: {"message":"Hello from FastAPI!","status":"success"}
```



#### 참고 코드안에서 직접실행가능

```python
from fastapi import FastAPI

app = FastAPI()

@app.get("/")
def hello() :
	return "Hello, World!"

if __name__ == "__main__" :
	uvicorn.run("main:app", reload=True)
```





