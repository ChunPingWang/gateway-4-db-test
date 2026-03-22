package com.example.gateway.interactor;

import com.example.gateway.entity.Employee;
import com.example.gateway.gateway.EmployeeGateway;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Business Rules 單元測試 — 使用 Stub 和 Spy 取代 GatewayImpl。
 *
 * 書中說明：
 * 「測試 Business Rules 時，請使用 Stub 和 Spy 來取代 GatewayImpl 類別。
 *  不容易出錯，反之，確保每個資料庫的情況下測試 Business Rules，
 *  這樣做速度很快，又容易做速度很慢。」
 *
 * 「反之，確保每個資料庫的情況下測試 Business Rules，應該要測試
 *  Business Rules 和 Interactor 是否正確地操作了 Gateway 介面。」
 *
 * 此測試完全不需要資料庫，使用手寫的 Stub/Spy 實作 EmployeeGateway 介面。
 * 這讓測試執行速度非常快，且能獨立驗證業務邏輯的正確性。
 */
@DisplayName("EmployeeInteractor 單元測試 — 使用 Stub/Spy 取代 Gateway")
class EmployeeInteractorTest {

    private EmployeeGatewayStubSpy gatewayStubSpy;
    private EmployeeInteractor interactor;

    /**
     * Stub + Spy 實作：
     * - Stub：提供預設的回傳值，模擬資料庫行為
     * - Spy：記錄被呼叫的方法與參數，供驗證用
     */
    static class EmployeeGatewayStubSpy implements EmployeeGateway {

        private final Map<Long, Employee> store = new HashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        // Spy: 記錄呼叫次數
        int saveCallCount = 0;
        int deleteCallCount = 0;
        Long lastDeletedId = null;

        @Override
        public Employee save(Employee employee) {
            saveCallCount++;
            if (employee.getId() == null) {
                employee.setId(idGenerator.getAndIncrement());
            }
            store.put(employee.getId(), employee);
            return employee;
        }

        @Override
        public Optional<Employee> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Employee> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<Employee> findHiredAfter(LocalDate date) {
            return store.values().stream()
                    .filter(e -> e.getHireDate().isAfter(date))
                    .toList();
        }

        @Override
        public List<Employee> findByDepartment(String department) {
            return store.values().stream()
                    .filter(e -> department.equals(e.getDepartment()))
                    .toList();
        }

        @Override
        public void deleteById(Long id) {
            deleteCallCount++;
            lastDeletedId = id;
            store.remove(id);
        }
    }

    @BeforeEach
    void setUp() {
        gatewayStubSpy = new EmployeeGatewayStubSpy();
        interactor = new EmployeeInteractor(gatewayStubSpy);
    }

    // ========== hireEmployee 業務規則測試 ==========

    @Test
    @DisplayName("hireEmployee: 應透過 Gateway 儲存新員工")
    void hireEmployee_shouldSaveThroughGateway() {
        Employee result = interactor.hireEmployee("Alice", LocalDate.of(2023, 1, 15), "Engineering");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getDepartment()).isEqualTo("Engineering");
        // Spy 驗證：確認 save 被呼叫了一次
        assertThat(gatewayStubSpy.saveCallCount).isEqualTo(1);
    }

    // ========== getEmployee 業務規則測試 ==========

    @Test
    @DisplayName("getEmployee: 應透過 Gateway 查詢員工")
    void getEmployee_shouldDelegateToGateway() {
        Employee hired = interactor.hireEmployee("Bob", LocalDate.of(2022, 6, 1), "Marketing");

        Optional<Employee> found = interactor.getEmployee(hired.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Bob");
    }

    // ========== findEmployeesHiredAfter 業務規則測試 ==========

    @Test
    @DisplayName("findEmployeesHiredAfter: 應正確委派給 Gateway 的 findHiredAfter")
    void findEmployeesHiredAfter_shouldDelegateToGateway() {
        interactor.hireEmployee("Old", LocalDate.of(2000, 1, 1), "Admin");
        interactor.hireEmployee("New", LocalDate.of(2023, 6, 1), "Engineering");

        List<Employee> result = interactor.findEmployeesHiredAfter(LocalDate.of(2001, 1, 1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("New");
    }

    // ========== transferEmployee 業務規則測試 ==========

    @Test
    @DisplayName("transferEmployee: 應更新員工部門並透過 Gateway 儲存")
    void transferEmployee_shouldUpdateDepartmentAndSave() {
        Employee hired = interactor.hireEmployee("Alice", LocalDate.of(2023, 1, 1), "Engineering");

        Employee transferred = interactor.transferEmployee(hired.getId(), "Management");

        assertThat(transferred.getDepartment()).isEqualTo("Management");
        // Spy 驗證：save 被呼叫了 2 次（hire + transfer）
        assertThat(gatewayStubSpy.saveCallCount).isEqualTo(2);
    }

    @Test
    @DisplayName("transferEmployee: 員工不存在時應拋出異常")
    void transferEmployee_shouldThrowWhenNotFound() {
        assertThatThrownBy(() -> interactor.transferEmployee(999L, "Marketing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Employee not found");
    }

    // ========== terminateEmployee 業務規則測試 ==========

    @Test
    @DisplayName("terminateEmployee: 應透過 Gateway 刪除員工")
    void terminateEmployee_shouldDeleteThroughGateway() {
        Employee hired = interactor.hireEmployee("ToFire", LocalDate.of(2023, 1, 1), "Temp");

        interactor.terminateEmployee(hired.getId());

        // Spy 驗證：delete 被呼叫了一次，且 ID 正確
        assertThat(gatewayStubSpy.deleteCallCount).isEqualTo(1);
        assertThat(gatewayStubSpy.lastDeletedId).isEqualTo(hired.getId());
        // Stub 驗證：員工已被移除
        assertThat(interactor.getEmployee(hired.getId())).isEmpty();
    }

    @Test
    @DisplayName("terminateEmployee: 員工不存在時應拋出異常")
    void terminateEmployee_shouldThrowWhenNotFound() {
        assertThatThrownBy(() -> interactor.terminateEmployee(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Employee not found");
    }

    // ========== findEmployeesByDepartment 業務規則測試 ==========

    @Test
    @DisplayName("findEmployeesByDepartment: 應正確委派給 Gateway")
    void findEmployeesByDepartment_shouldDelegateToGateway() {
        interactor.hireEmployee("Alice", LocalDate.of(2023, 1, 1), "Engineering");
        interactor.hireEmployee("Bob", LocalDate.of(2023, 2, 1), "Marketing");
        interactor.hireEmployee("Charlie", LocalDate.of(2023, 3, 1), "Engineering");

        List<Employee> engineers = interactor.findEmployeesByDepartment("Engineering");

        assertThat(engineers).hasSize(2);
        assertThat(engineers).extracting(Employee::getName)
                .containsExactlyInAnyOrder("Alice", "Charlie");
    }
}
