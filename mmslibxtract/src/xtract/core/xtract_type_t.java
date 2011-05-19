/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.31
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xtract.core;

public final class xtract_type_t {
  public final static xtract_type_t XTRACT_FLOAT = new xtract_type_t("XTRACT_FLOAT");
  public final static xtract_type_t XTRACT_FLOATARRAY = new xtract_type_t("XTRACT_FLOATARRAY");
  public final static xtract_type_t XTRACT_INT = new xtract_type_t("XTRACT_INT");
  public final static xtract_type_t XTRACT_MEL_FILTER = new xtract_type_t("XTRACT_MEL_FILTER");

  public final int swigValue() {
    return swigValue;
  }

  public String toString() {
    return swigName;
  }

  public static xtract_type_t swigToEnum(int swigValue) {
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (int i = 0; i < swigValues.length; i++)
      if (swigValues[i].swigValue == swigValue)
        return swigValues[i];
    throw new IllegalArgumentException("No enum " + xtract_type_t.class + " with value " + swigValue);
  }

  private xtract_type_t(String swigName) {
    this.swigName = swigName;
    this.swigValue = swigNext++;
  }

  private xtract_type_t(String swigName, int swigValue) {
    this.swigName = swigName;
    this.swigValue = swigValue;
    swigNext = swigValue+1;
  }

  private xtract_type_t(String swigName, xtract_type_t swigEnum) {
    this.swigName = swigName;
    this.swigValue = swigEnum.swigValue;
    swigNext = this.swigValue+1;
  }

  private static xtract_type_t[] swigValues = { XTRACT_FLOAT, XTRACT_FLOATARRAY, XTRACT_INT, XTRACT_MEL_FILTER };
  private static int swigNext = 0;
  private final int swigValue;
  private final String swigName;
}
