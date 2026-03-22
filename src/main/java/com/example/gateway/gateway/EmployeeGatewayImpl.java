package com.example.gateway.gateway;

import com.example.gateway.entity.Employee;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Gateway IMPL — 實作 EmployeeGateway 介面，實際連接資料庫。
 *
 * 對應書中圖 4.1 的 "Gateway IMPL → DB" 角色。
 * 書中說明：「GatewayImpl 類別實作了閘道，並指示（即控制）實際的資料庫去執行所需的函數。
 * 如果使用的是 SQL 資料庫，那麼所有的 SQL 都是在這個 GatewayImpl 類別中建立的。
 * 如果使用的是 ORM，那麼這個 GatewayImpl 類別會操作 ORM。」
 *
 * 測試此類別時，需要連接真實的測試資料庫（Testcontainers）。
 */
@Component
public class EmployeeGatewayImpl implements EmployeeGateway {

    private final EmployeeJpaRepository repository;

    public EmployeeGatewayImpl(EmployeeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Employee save(Employee employee) {
        return repository.save(employee);
    }

    @Override
    public Optional<Employee> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Employee> findAll() {
        return repository.findAll();
    }

    @Override
    public List<Employee> findHiredAfter(LocalDate date) {
        return repository.findByHireDateAfter(date);
    }

    @Override
    public List<Employee> findByDepartment(String department) {
        return repository.findByDepartment(department);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
