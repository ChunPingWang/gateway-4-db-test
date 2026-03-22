package com.example.gateway.gateway;

import com.example.gateway.entity.Employee;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Gateway 介面 — 定義所有我們希望對員工資料執行的查詢與操作。
 *
 * 對應書中圖 4.1 的 "Gateway + Methods..." 角色。
 * 書中提到：「在 Gateway 介面中，每一種我們希望執行的查詢都有一個對應的方法。」
 *
 * Interactor（Business Rules）僅依賴此介面，不知道底層是 SQL、ORM 還是其他實作。
 * 測試 Business Rules 時，可用 Stub/Spy 取代此介面的實作。
 */
public interface EmployeeGateway {

    Employee save(Employee employee);

    Optional<Employee> findById(Long id);

    List<Employee> findAll();

    List<Employee> findHiredAfter(LocalDate date);

    List<Employee> findByDepartment(String department);

    void deleteById(Long id);
}
