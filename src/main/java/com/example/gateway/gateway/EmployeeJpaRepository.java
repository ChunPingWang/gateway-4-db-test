package com.example.gateway.gateway;

import com.example.gateway.entity.Employee;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA Repository — 提供 ORM 層級的資料庫存取能力。
 * 這是 GatewayImpl 內部使用的基礎設施細節，不對外暴露。
 */
public interface EmployeeJpaRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByHireDateAfter(LocalDate date);

    List<Employee> findByDepartment(String department);
}
