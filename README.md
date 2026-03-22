# Gateway Pattern 資料庫測試示範

本專案示範 **Gateway Pattern（閘道模式）** 在 Spring JPA 與 Testcontainers 下的實作與測試策略，取材自 Robert C. Martin《Clean Craftsmanship》第四章。

---

## 專案架構總覽

```
src/main/java/com/example/gateway/
├── GatewayPatternApplication.java      # Spring Boot 啟動入口
├── entity/
│   └── Employee.java                   # JPA Entity（Business Object）
├── gateway/
│   ├── EmployeeGateway.java            # Gateway 介面（抽象邊界）
│   ├── EmployeeGatewayImpl.java        # Gateway 實作（操作 ORM）
│   └── EmployeeJpaRepository.java      # Spring Data JPA Repository
└── interactor/
    └── EmployeeInteractor.java         # Interactor（Business Rules）

src/test/java/com/example/gateway/
├── gateway/
│   └── EmployeeGatewayImplIntegrationTest.java  # 整合測試（Testcontainers）
└── interactor/
    └── EmployeeInteractorTest.java              # 單元測試（Stub/Spy）
```

---

## Class Diagram

```mermaid
classDiagram
    direction LR

    class Employee {
        -Long id
        -String name
        -LocalDate hireDate
        -String department
        +getId() Long
        +setId(Long)
        +getName() String
        +setName(String)
        +getHireDate() LocalDate
        +setHireDate(LocalDate)
        +getDepartment() String
        +setDepartment(String)
    }

    class EmployeeGateway {
        <<interface>>
        +save(Employee) Employee
        +findById(Long) Optional~Employee~
        +findAll() List~Employee~
        +findHiredAfter(LocalDate) List~Employee~
        +findByDepartment(String) List~Employee~
        +deleteById(Long)
    }

    class EmployeeGatewayImpl {
        -EmployeeJpaRepository repository
        +save(Employee) Employee
        +findById(Long) Optional~Employee~
        +findAll() List~Employee~
        +findHiredAfter(LocalDate) List~Employee~
        +findByDepartment(String) List~Employee~
        +deleteById(Long)
    }

    class EmployeeJpaRepository {
        <<interface>>
        +findByHireDateAfter(LocalDate) List~Employee~
        +findByDepartment(String) List~Employee~
    }

    class EmployeeInteractor {
        -EmployeeGateway employeeGateway
        +hireEmployee(String, LocalDate, String) Employee
        +getEmployee(Long) Optional~Employee~
        +getAllEmployees() List~Employee~
        +findEmployeesHiredAfter(LocalDate) List~Employee~
        +findEmployeesByDepartment(String) List~Employee~
        +transferEmployee(Long, String) Employee
        +terminateEmployee(Long)
    }

    EmployeeInteractor --> EmployeeGateway : 依賴介面
    EmployeeGatewayImpl ..|> EmployeeGateway : 實作
    EmployeeGatewayImpl --> EmployeeJpaRepository : 委派
    EmployeeJpaRepository --> Employee : 操作
    EmployeeGateway ..> Employee : 使用
```

### 設計重點

| 角色 | 類別 | 書中對應 |
|------|------|----------|
| **Business Object** | `Employee` | 領域物件，承載業務資料 |
| **Gateway（介面）** | `EmployeeGateway` | 定義資料存取契約，隔離業務邏輯與資料庫 |
| **Gateway IMPL** | `EmployeeGatewayImpl` | 實作閘道，操作 ORM / SQL |
| **Interactor** | `EmployeeInteractor` | 業務規則，僅依賴 Gateway 介面 |
| **Infrastructure** | `EmployeeJpaRepository` | Spring Data JPA，屬於 GatewayImpl 內部細節 |

---

## Sequence Diagram

### 場景一：聘僱員工（hireEmployee）

```mermaid
sequenceDiagram
    participant Client
    participant Interactor as EmployeeInteractor
    participant Gateway as EmployeeGateway
    participant Impl as EmployeeGatewayImpl
    participant Repo as EmployeeJpaRepository
    participant DB as PostgreSQL

    Client->>Interactor: hireEmployee("Alice", 2023-01-15, "Engineering")
    Interactor->>Interactor: new Employee("Alice", ...)
    Interactor->>Gateway: save(employee)
    Gateway->>Impl: save(employee)
    Impl->>Repo: save(employee)
    Repo->>DB: INSERT INTO employees ...
    DB-->>Repo: 回傳已儲存記錄
    Repo-->>Impl: Employee(id=1, ...)
    Impl-->>Gateway: Employee(id=1, ...)
    Gateway-->>Interactor: Employee(id=1, ...)
    Interactor-->>Client: Employee(id=1, ...)
```

### 場景二：轉調部門（transferEmployee）

```mermaid
sequenceDiagram
    participant Client
    participant Interactor as EmployeeInteractor
    participant Gateway as EmployeeGateway
    participant Impl as EmployeeGatewayImpl
    participant Repo as EmployeeJpaRepository
    participant DB as PostgreSQL

    Client->>Interactor: transferEmployee(1, "Management")
    Interactor->>Gateway: findById(1)
    Gateway->>Impl: findById(1)
    Impl->>Repo: findById(1)
    Repo->>DB: SELECT ... WHERE id=1
    DB-->>Repo: Employee(id=1, dept="Engineering")
    Repo-->>Impl: Optional[Employee]
    Impl-->>Gateway: Optional[Employee]
    Gateway-->>Interactor: Optional[Employee]
    Interactor->>Interactor: employee.setDepartment("Management")
    Interactor->>Gateway: save(employee)
    Gateway->>Impl: save(employee)
    Impl->>Repo: save(employee)
    Repo->>DB: UPDATE employees SET department='Management' WHERE id=1
    DB-->>Repo: 更新完成
    Repo-->>Impl: Employee(id=1, dept="Management")
    Impl-->>Interactor: Employee(id=1, dept="Management")
    Interactor-->>Client: Employee(id=1, dept="Management")
```

### 場景三：單元測試時（Stub/Spy 取代 GatewayImpl）

```mermaid
sequenceDiagram
    participant Test as EmployeeInteractorTest
    participant Interactor as EmployeeInteractor
    participant StubSpy as EmployeeGatewayStubSpy
    participant Memory as HashMap（記憶體）

    Note over Test,Memory: 不需要資料庫！速度極快

    Test->>Interactor: hireEmployee("Alice", ...)
    Interactor->>StubSpy: save(employee)
    StubSpy->>StubSpy: saveCallCount++
    StubSpy->>Memory: store.put(id, employee)
    Memory-->>StubSpy: 儲存完成
    StubSpy-->>Interactor: Employee(id=1, ...)
    Interactor-->>Test: Employee(id=1, ...)
    Test->>Test: assert saveCallCount == 1（Spy 驗證）
```

---

## ER Diagram

```mermaid
erDiagram
    EMPLOYEES {
        bigserial id PK "自動遞增主鍵"
        varchar name "員工姓名"
        date hire_date "聘僱日期"
        varchar department "所屬部門"
    }
```

### 欄位對應

| Java 欄位 | 資料庫欄位 | 型別 | 說明 |
|-----------|-----------|------|------|
| `id` | `id` | `bigserial` | 自動遞增主鍵（`@GeneratedValue(IDENTITY)`） |
| `name` | `name` | `varchar(255)` | 員工姓名 |
| `hireDate` | `hire_date` | `date` | 聘僱日期（Spring 自動轉換 camelCase → snake_case） |
| `department` | `department` | `varchar(255)` | 所屬部門 |

---

## 測試策略

本專案完整實踐書中描述的**兩層測試策略**：

```
┌─────────────────────────────────────────────────────────┐
│  單元測試（快速、無資料庫）                                  │
│  EmployeeInteractorTest                                 │
│  ┌───────────────┐     ┌─────────────────────────┐      │
│  │  Interactor   │────▶│  EmployeeGatewayStubSpy │      │
│  │ (Business     │     │  (Stub: 模擬回傳值)       │      │
│  │  Rules)       │     │  (Spy: 記錄呼叫行為)      │      │
│  └───────────────┘     └─────────────────────────┘      │
│  驗證重點：業務邏輯是否正確操作 Gateway 介面                  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  整合測試（需要真實資料庫）                                  │
│  EmployeeGatewayImplIntegrationTest                     │
│  ┌───────────────┐     ┌──────────────┐     ┌────────┐  │
│  │  GatewayImpl  │────▶│ JpaRepository│────▶│  PostgreSQL │
│  └───────────────┘     └──────────────┘     │(Testcontainers)│
│  驗證重點：SQL / ORM 查詢是否正確運作                       │
└─────────────────────────────────────────────────────────┘
```

### 書中核心觀點

> **「測試 Business Rules 時，請使用 Stub 和 Spy 來取代 GatewayImpl 類別。」**
>
> 業務邏輯測試不應該依賴資料庫，這樣做速度快、不容易出錯。

> **「測試資料庫這件事就變得很簡單了。你可以建立一個相當簡單的測試資料庫，然後呼叫 GatewayImpl 的每個查詢函數，確保這些函數對測試資料庫產生預期的效果。」**
>
> Gateway 實作的整合測試只需驗證 SQL/ORM 是否正確，不涉及業務邏輯。

### 測試涵蓋率

| 類別 | 指令涵蓋率 | 行涵蓋率 | 方法涵蓋率 |
|------|-----------|---------|-----------|
| EmployeeGatewayImpl | 100% | 100% | 100% |
| EmployeeInteractor | 95% | 94% | 90% |
| Employee | 91% | 89% | 90% |
| **整體** | **92%** | **91%** | — |

---

## 技術棧

| 技術 | 用途 |
|------|------|
| Spring Boot 3.2.5 | 應用程式框架 |
| Spring Data JPA | ORM 資料存取 |
| Hibernate 6.4 | JPA 實作 |
| PostgreSQL 16 | 資料庫 |
| Testcontainers 1.19.7 | 自動啟動測試用 PostgreSQL 容器 |
| JUnit 5 | 測試框架 |
| AssertJ | 流暢斷言 |

---

## 快速開始

```bash
# 前置需求：Docker（Testcontainers 需要）、Java 17+

# 執行所有測試
mvn test

# 僅執行單元測試（不需要 Docker）
mvn test -Dtest=EmployeeInteractorTest

# 僅執行整合測試（需要 Docker）
mvn test -Dtest=EmployeeGatewayImplIntegrationTest
```

---

## 學習資源

- **書籍**：Robert C. Martin,《Clean Craftsmanship》第四章 — Test Design
- **模式**：Gateway Pattern（Martin Fowler, *Patterns of Enterprise Application Architecture*）
- **測試替身**：Stub、Spy、Mock 的區別與使用時機（Gerard Meszaros, *xUnit Test Patterns*）
- **Testcontainers**：[官方文件](https://testcontainers.com/)
