package com.example.gateway.interactor;

import com.example.gateway.entity.Employee;
import com.example.gateway.gateway.EmployeeGateway;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Interactor（Business Rules）— 包含員工相關的業務邏輯。
 *
 * 對應書中圖 4.1 的 "Interactor → Business Rules" 角色。
 * 此類別僅依賴 EmployeeGateway「介面」，完全不知道資料庫的存在。
 *
 * 書中提到：「測試 Business Rules 時，請使用 Stub 和 Spy 來取代 GatewayImpl 類別。」
 * 因此測試此類別時，不需要真實資料庫，只需提供 Gateway 介面的假實作。
 */
@Service
public class EmployeeInteractor {

    private final EmployeeGateway employeeGateway;

    public EmployeeInteractor(EmployeeGateway employeeGateway) {
        this.employeeGateway = employeeGateway;
    }

    public Employee hireEmployee(String name, LocalDate hireDate, String department) {
        Employee employee = new Employee(name, hireDate, department);
        return employeeGateway.save(employee);
    }

    public Optional<Employee> getEmployee(Long id) {
        return employeeGateway.findById(id);
    }

    public List<Employee> getAllEmployees() {
        return employeeGateway.findAll();
    }

    /**
     * 書中範例：找出某年份之後被聘僱的員工。
     * 例如 hiredAfter(2001) — 可呼叫 Gateway.findHiredAfter(2001) 方法。
     */
    public List<Employee> findEmployeesHiredAfter(LocalDate date) {
        return employeeGateway.findHiredAfter(date);
    }

    public List<Employee> findEmployeesByDepartment(String department) {
        return employeeGateway.findByDepartment(department);
    }

    public Employee transferEmployee(Long employeeId, String newDepartment) {
        Employee employee = employeeGateway.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        employee.setDepartment(newDepartment);
        return employeeGateway.save(employee);
    }

    public void terminateEmployee(Long employeeId) {
        employeeGateway.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        employeeGateway.deleteById(employeeId);
    }
}
