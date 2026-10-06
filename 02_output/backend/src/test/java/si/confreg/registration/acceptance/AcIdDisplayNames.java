package si.confreg.registration.acceptance;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayNameGenerator;

/** Shows test methods named {@code ac_001_01_xyz} as {@code AC-001-01 xyz} in reports. */
class AcIdDisplayNames extends DisplayNameGenerator.Standard {

  private static final Pattern AC_METHOD = Pattern.compile("^ac_(\\d{3})_(\\d{2})_(.*)$");

  @Override
  public String generateDisplayNameForMethod(
      List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
    Matcher matcher = AC_METHOD.matcher(testMethod.getName());
    if (matcher.matches()) {
      return "AC-" + matcher.group(1) + "-" + matcher.group(2) + " " + matcher.group(3);
    }
    return super.generateDisplayNameForMethod(enclosingInstanceTypes, testClass, testMethod);
  }
}
