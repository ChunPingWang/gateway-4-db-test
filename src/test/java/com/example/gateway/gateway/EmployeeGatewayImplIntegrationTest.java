package com.example.gateway.gateway;

import com.example.gateway.entity.Employee;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gateway IMPL 整合測試 — 連接真實的測試資料庫（Testcontainers PostgreSQL）。
 *
 * 書中說明：
 * 「測試資料庫這件事就變得很簡單了。你可以建立一個相當簡單的測試資料庫，
 *  然後在資料庫中呼叫 GatewayImpl 的每個查詢函數（query function），
 *  確保這些函數對測試資料庫產生預期的效果。確保每個查詢函數都能回傳、
 *  確保每個更新、新增和刪除操作都能正確地變更資料庫。」
 *
 * 「不要使用正式環境的資料庫來進行這些測試。請建立一個擁有足夠資料列數的
 *  測試資料庫，證明測試有效，然後備份這個資料庫。在執行測試之前，請還原
 *  這個備份的測試資料。」
 *
 * 此處使用 Testcontainers 自動啟動一個乾淨的 PostgreSQL 容器，
 * 每次測試都從全新狀態開始，完美實現書中描述的「相同的測試資料」概念。
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EmployeeGatewayImpl.class)
@DisplayName("EmployeeGatewayImpl 整合測試 — 連接真實測試資料庫")
class EmployeeGatewayImplIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private EmployeeGateway gateway;

    @Autowired
    private EmployeeJpaRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // ========== 新增（Create）測試 ==========

    @Test
    @DisplayName("save: 新增員工後應能正確儲存至資料庫")
    void save_shouldPersistEmployee() {
        Employee employee = new Employee("Alice", LocalDate.of(2020, 1, 15), "Engineering");

        Employee saved = gateway.save(employee);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getHireDate()).isEqualTo(LocalDate.of(2020, 1, 15));
        assertThat(saved.getDepartment()).isEqualTo("Engineering");
    }

    // ========== 查詢（Read）測試 ==========

    @Test
    @DisplayName("findById: 依 ID 查詢應回傳正確的員工")
    void findById_shouldReturnEmployee() {
        Employee saved = gateway.save(new Employee("Bob", LocalDate.of(2019, 6, 1), "Marketing"));

        Optional<Employee> found = gateway.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("findById: 查詢不存在的 ID 應回傳空")
    void findById_shouldReturnEmptyForNonExistent() {
        Optional<Employee> found = gateway.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findAll: 應回傳所有員工")
    void findAll_shouldReturnAllEmployees() {
        gateway.save(new Employee("Alice", LocalDate.of(2020, 1, 1), "Engineering"));
        gateway.save(new Employee("Bob", LocalDate.of(2021, 3, 1), "Marketing"));
        gateway.save(new Employee("Charlie", LocalDate.of(2022, 5, 1), "Engineering"));

        List<Employee> all = gateway.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("findHiredAfter: 應回傳指定日期之後聘僱的員工（書中範例 hiredAfter(2001)）")
    void findHiredAfter_shouldReturnEmployeesHiredAfterDate() {
        gateway.save(new Employee("Old Timer", LocalDate.of(2000, 1, 1), "Admin"));
        gateway.save(new Employee("Mid Career", LocalDate.of(2001, 6, 15), "Engineering"));
        gateway.save(new Employee("New Hire", LocalDate.of(2023, 1, 1), "Engineering"));

        List<Employee> result = gateway.findHiredAfter(LocalDate.of(2001, 1, 1));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Employee::getName)
                .containsExactlyInAnyOrder("Mid Career", "New Hire");
    }

    @Test
    @DisplayName("findByDepartment: 應回傳指定部門的員工")
    void findByDepartment_shouldReturnEmployeesInDepartment() {
        gateway.save(new Employee("Alice", LocalDate.of(2020, 1, 1), "Engineering"));
        gateway.save(new Employee("Bob", LocalDate.of(2021, 3, 1), "Marketing"));
        gateway.save(new Employee("Charlie", LocalDate.of(2022, 5, 1), "Engineering"));

        List<Employee> engineers = gateway.findByDepartment("Engineering");

        assertThat(engineers).hasSize(2);
        assertThat(engineers).extracting(Employee::getName)
                .containsExactlyInAnyOrder("Alice", "Charlie");
    }

    // ========== 更新（Update）測試 ==========

    @Test
    @DisplayName("save: 更新員工資料後應正確反映變更")
    void save_shouldUpdateExistingEmployee() {
        Employee saved = gateway.save(new Employee("Alice", LocalDate.of(2020, 1, 1), "Engineering"));

        saved.setDepartment("Management");
        saved.setName("Alice Chen");
        gateway.save(saved);

        Optional<Employee> updated = gateway.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("Alice Chen");
        assertThat(updated.get().getDepartment()).isEqualTo("Management");
    }

    // ========== 刪除（Delete）測試 ==========

    @Test
    @DisplayName("deleteById: 刪除員工後應無法再查詢到")
    void deleteById_shouldRemoveEmployee() {
        Employee saved = gateway.save(new Employee("ToDelete", LocalDate.of(2020, 1, 1), "Temp"));

        gateway.deleteById(saved.getId());

        Optional<Employee> found = gateway.findById(saved.getId());
        assertThat(found).isEmpty();
    }
}
