package kh.edu.paragoniu.SpringBoot.Repos;

import kh.edu.paragoniu.SpringBoot.Model.Issue;
import kh.edu.paragoniu.SpringBoot.Model.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByStatus(IssueStatus status);
    List<Issue> findAllByOrderByIssueDateDescIdDesc();
    List<Issue> findByUserIdOrderByIssueDateDescIdDesc(Long userId);
}
