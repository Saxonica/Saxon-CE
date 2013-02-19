package org.timepedia.exporter.rebind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportJsInitMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.ExporterUtil;
import org.timepedia.exporter.client.SType;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 *
 */
public class JExportableClassType implements JExportable, JExportableType {

  private static final String IMPL_EXTENSION = "ExporterImpl";

  private ExportableTypeOracle exportableTypeOracle;

  private JClassType type;

  public JExportableClassType(ExportableTypeOracle exportableTypeOracle,
      JClassType type) {
    this.exportableTypeOracle = exportableTypeOracle;

    this.type = type;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    JExportableClassType that = (JExportableClassType) o;

    return getQualifiedSourceName().equals(that.getQualifiedSourceName());
  }

  public String[] getEnclosingClasses() {
    String[] enc = type.getName().split("\\.");
    String[] enclosingTypes = new String[enc.length - 1];
    if (enc.length > 1) {
      System.arraycopy(enc, 0, enclosingTypes, 0, enclosingTypes.length);
    }
    return enclosingTypes;
  }

  public JExportableConstructor[] getExportableConstructors() {
    ArrayList<JExportableConstructor> exportableCons = new ArrayList<JExportableConstructor>();
    for (JConstructor method : type.getConstructors()) {
      if (exportableTypeOracle.isExportable(method, this)) {
        exportableCons.add(new JExportableConstructor(this, method));
      }
    }
    return exportableCons.toArray(new JExportableConstructor[0]);
  }
  
  public JExportableMethod[] getExportableFactoryMethods() {
   ArrayList<JExportableMethod> exportableMethods = new ArrayList<JExportableMethod>();
   JType retClass = getTypeToExport();
   for (JMethod method : type.getMethods()) {
     if (exportableTypeOracle.isExportableFactoryMethod(method, retClass)) {
       exportableMethods.add(new JExportableMethod(this, method));
     }
   }
   return exportableMethods.toArray(new JExportableMethod[0]);
  }
  
  public JExportableField[] getExportableFields() {
    ArrayList<JExportableField> exportableFields
        = new ArrayList<JExportableField>();

    for (JField field : type.getFields()) {
      if (exportableTypeOracle.isExportable(field)) {
        exportableFields.add(new JExportableField(this, field));
      }
    }
    return exportableFields.toArray(new JExportableField[0]);
  }

  private JExportableMethod[] exportableMethods;
  
  private JExportableMethod jsInitMethod;
  private JExportableMethod afterCreateMethod;
  
  public JExportableMethod[] getExportableMethods() {
    if (exportableMethods == null) {
      ArrayList<JExportableMethod> ret = new ArrayList<JExportableMethod>();

      // Create a set with all methods in this class
      HashSet<JMethod> classMethods = new HashSet<JMethod>();
      classMethods.addAll(Arrays.asList(type.getMethods()));
      classMethods.addAll(Arrays.asList(type.getInheritableMethods()));
      
      for (JMethod method : classMethods) {
        if (exportableTypeOracle.isConstructor(method, this)) {
          continue;
        }
        
        if (exportableTypeOracle.isExportable(method, this)) {
          JExportableMethod m = new JExportableMethod(this, method);
          if (m.isExportJsInitMethod() 
              && exportableTypeOracle.isJavaScriptObject(method.getReturnType())
              ) {
            jsInitMethod = m;
          }
          if (m.isExportAfterCreateMethod()) {
            afterCreateMethod = m;
          } else {
            ret.add(m);
          }
        }
      }
      exportableMethods = ret.toArray(new JExportableMethod[0]);
    }
    return exportableMethods;
  }
  
  public JExportableMethod getJsInitMethod() {
    return jsInitMethod;
  }
  
  public JExportableMethod getAfterCreateMethod() {
    return afterCreateMethod;
  }

  public ExportableTypeOracle getExportableTypeOracle() {
    return exportableTypeOracle;
  }

  public String getExporterImplementationName() {
    return type.getSimpleSourceName() + IMPL_EXTENSION;
  }

  public String getHostedModeJsTypeCast() {
    return null;
  }

  public String getJsTypeOf() {
    return exportableTypeOracle.getJsTypeOf(getType());
  }

  public String getJSConstructor() {
    String pkg = getJSExportPackage().trim();
    if (!pkg.isEmpty()) {
      pkg += ".";
    }
    return pkg + getJSExportName();
  }
  
  public String getJSExportName () {
    Export ann = type.getAnnotation(Export.class);
    JClassType type = getTypeToExport();
    return ann != null && !ann.value().isEmpty() ? ann.value() : type.getName();
  }

  public String getJSExportPackage() {
    String requestedPackageName = getPrefix();
    ExportPackage ann = type.getAnnotation(ExportPackage.class);
    JClassType type = getTypeToExport();
    if (ann != null) {
      requestedPackageName = ann.value();
    } else if (type.getEnclosingType() != null) {
      JExportableClassType encType = exportableTypeOracle
          .findExportableClassType(
              type.getEnclosingType().getQualifiedSourceName());
      if (encType != null) {
        return encType.getJSExportPackage();
      }
    }
    return requestedPackageName;
  }

  public String getJSNIReference() {
    return type.getJNISignature();
  }

  public String getJSQualifiedExportName() {
    return getJSConstructor();
  }

  public String getPackageName() {
    return type.getPackage().getName();
  }

  public String getPrefix() {
    String prefix = "";
    boolean firstClientPackage = true;
    for (String pkg : type.getPackage().getName().split("\\.")) {
      if (firstClientPackage && pkg.equals("client")) {
        firstClientPackage = false;
        continue;
      }
      prefix += pkg;
      prefix += '.';
    }
    // remove trailing .
    return prefix.substring(0, prefix.length() - 1);
  }

  public String getQualifiedExporterImplementationName() {
    return getPackageName() + "." + getExporterImplementationName();
  }

  public String getQualifiedSourceName() {
    return getType().getQualifiedSourceName();
  }

  public JStructuralTypeField[] getStructuralTypeFields() {
    if (!isStructuralType()) {
      return new JStructuralTypeField[0];
    } else {
      ArrayList<JStructuralTypeField> fields
          = new ArrayList<JStructuralTypeField>();
      for (JMethod method : type.getMethods()) {
        if (method.getName().startsWith("set")
            && Character.isUpperCase(method.getName().charAt(3))
            && method.getParameters().length == 1
            || method.getAnnotation(SType.class) != null) {
          fields.add(new JStructuralTypeField(this, method));
        }
      }
      return fields.toArray(new JStructuralTypeField[0]);
    }
  }

  public JClassType getType() {
    return type;
  }

  public JClassType getRequestedType() {
    return type;
  }

  public JClassType getTypeToExport() {
    return exportableTypeOracle.isExportOverlay(type)
        ? exportableTypeOracle.getExportOverlayType(type) : type;
  }

  public String getWrapperFunc() {
    if (!needsExport()) {
      return null;
    }
    return "@" + ExporterUtil.class.getName()
          + "::wrap(Ljava/lang/Object;)";
  }

  public int hashCode() {
    return getQualifiedSourceName().hashCode();
  }

  public boolean isPrimitive() {
    return type.isPrimitive() != null;
  }

  public boolean isStructuralType() {
    return exportableTypeOracle.isStructuralType(this.getType());
  }

  public boolean isTransparentType() {
    return exportableTypeOracle.isJavaScriptObject(this)
        || exportableTypeOracle.isString(this) 
        || exportableTypeOracle.isDate(this) 
        || exportableTypeOracle.isArray(this);
  }
  
  public String getJsniSigForArrays() {
    if (exportableTypeOracle.isJavaScriptObject(this)) {
      return "[Lcom/google/gwt/core/client/JavaScriptObject;";
    } else if (isTransparentType()){
      return "[" + type.getJNISignature();
    } else {
      return "[Lorg/timepedia/exporter/client/Exportable;";
    }
  }

  public boolean needsExport() {
    return !isPrimitive() && !isTransparentType();
  }

  public boolean isInstantiable() {
    return exportableTypeOracle.isInstantiable(type);                
  }
  
}
