package dev.library.teacher;
public record TeacherResponse(Long id, String name, String extension, String phone, String position, String location) {
  public static TeacherResponse from(Teacher teacher) {
    return new TeacherResponse(teacher.getId(), teacher.getName(), teacher.getExtension(), teacher.getPhone(), teacher.getPosition(), teacher.getLocation());
  }
}
