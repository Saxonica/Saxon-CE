package org.timepedia.exporter.rebind;

import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;

/**
 *
 */
public class JExportableParameter {

  private JParameter param;

  public JParameter getParam() {
    return param;
  }

  private JExportableClassType exportableEnclosingType;

  public JExportableClassType getExportableEnclosingType() {
    return exportableEnclosingType;
  }

  public JExportableParameter(JExportableMethod exportableMethod,
      JParameter param) {

    this.param = param;
    this.exportableEnclosingType = exportableMethod.getEnclosingExportType();
  }

  public String getTypeName() {
    return param.getType().getQualifiedSourceName();
  }
  
  public String getJNISignature() {
    return param.getType().getJNISignature();
  }

  public String getExportParameterValue(String argName) {
    ExportableTypeOracle xTypeOracle = exportableEnclosingType
        .getExportableTypeOracle();
    
    String paramTypeName = param.getType().getQualifiedSourceName();
    JExportableType type = xTypeOracle.findExportableType(paramTypeName);
    
    if (type != null && type.needsExport()) {
      JExportableClassType cType = (JExportableClassType) type;
      if (xTypeOracle.isClosure((JExportableClassType) type)) {
        return argName + " == null ? null : (" + argName + ".constructor == $wnd."
            + cType.getJSQualifiedExportName() + " ? " + argName
            + "." + ClassExporter.GWT_INSTANCE + " : " 
            + "@" + cType.getQualifiedExporterImplementationName() + "::"
            + "makeClosure(Lcom/google/gwt/core/client/JavaScriptObject;)("
            + argName 
            + "))";
      }
    }
    
    if (param.getType().isClass() != null
        && !xTypeOracle.isJavaScriptObject(param.getType())
        && !xTypeOracle.isString(param.getType())) {
      return "@org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)("
          + argName + ")";
    }

    return argName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    JExportableParameter that = (JExportableParameter) o;
    return getJsTypeOf().equals(that.getJsTypeOf());
  }

  public String getJsTypeOf() {
    ExportableTypeOracle xto = exportableEnclosingType.getExportableTypeOracle();

    if (param == null) {
      return "null";
    } else if (param.getType().isArray() != null) {
      return "array";
    } else if (param.getType().isPrimitive() != null) {
      JPrimitiveType prim = param.getType().isPrimitive();
      return prim == JPrimitiveType.BOOLEAN ? "boolean" : "number";
    } else if (xto.isString(param.getType())) {
      return "string";
    } else if (xto.isJavaScriptObject(param.getType())) {
      return "object";
    } else {
      String paramTypeName = param.getType().getQualifiedSourceName();
      JExportableType type = xto.findExportableType(paramTypeName);
      if (type != null && type instanceof JExportableClassType
          && xto.isClosure((JExportableClassType) type)) {
        return "'function'";
      }
      return "@" + param.getType().getQualifiedSourceName() + "::class";
    }
  }
  
  public boolean isExportable() {
    String js = getJsTypeOf();
    return !js.contains("@") || getExportableType() != null;
  }

  @Override
  public int hashCode() {
    return param != null ? getJsTypeOf().hashCode() : 0;
  }

  public String toString() {
    return param.getType().getSimpleSourceName();
  }

  public JExportableType getExportableType() {
    return exportableEnclosingType.getExportableTypeOracle()
        .findExportableClassType(getTypeName());
  }
  
  public String getToArrayFunc(String qsn, String argName) {
    String ret = "ExporterUtil.";
    String after = ")";
    ExportableTypeOracle o = exportableEnclosingType.getExportableTypeOracle();
    JExportableType t = o.findExportableType(qsn.replace("[]", ""));
    JExportableClassType e = null;
    if (t != null && (t instanceof JExportableClassType)) {
      e = (JExportableClassType) t; 
    }
    if (qsn.equals("java.lang.String[]")) {
      ret += "toArrString" ;
    } else if (qsn.equals("java.util.Date[]")) {
      ret += "toArrDate" ;
    } else if (qsn.equals("double[]")) {
      ret += "toArrDouble" ;
    } else if (qsn.equals("float[]")) {
      ret += "toArrFloat" ;
    } else if (qsn.equals("long[]")) {
      ret += "toArrLong" ;
    } else if (qsn.equals("int[]")) {
      ret += "toArrInt" ;
    } else if (qsn.equals("byte[]")) {
      ret += "toArrByte" ;
    } else if (qsn.equals("char[]")) {
      ret += "toArrChar" ;
    } else {
      ret += "toArrObject";
      after = ", new " + qsn.replace("]", "ExporterUtil.length(" + argName + ")]") + after; 
//      if (e != null && o.isJavaScriptObject(e)) {
//        ret += "toArrJsObject";
//      } else if (t != null) {
//        ret += "toArrExport" ;
//      } else {
//        ret += "toArrObject" ;
//      }
    }
    return ret + "(" + argName + after;
  }

}
