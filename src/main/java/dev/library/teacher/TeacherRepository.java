package dev.library.teacher;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
  List<Teacher> findAllByOrderByIdAsc();
}
