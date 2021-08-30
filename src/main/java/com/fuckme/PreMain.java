package com.fuckme;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

public class PreMain {
    public static void premain(String agentArgs, Instrumentation inst) {
        if (agentArgs == null) {
            System.out.println("[CSAgent] Agent options not found!");
            return;
        }
        inst.addTransformer(new CobaltStrikeTransformer(agentArgs), true);
    }

    static class CobaltStrikeTransformer implements ClassFileTransformer {
        private final ClassPool classPool = ClassPool.getDefault();
        private final String hexkey;
        private final Boolean needTranslation;

        public CobaltStrikeTransformer(String args) {
            this.hexkey = args;
            this.needTranslation = Files.exists(Paths.get("resources/translation.txt"));
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            // System.out.println("premain load Class:" + className);
            try {
                if (className == null) {
                    return classfileBuffer;
                    // } else if (className.equals("beacon/BeaconData")) {
                    //     // 暗桩修复，修改zip包后，30分钟所有命令都会变成exit，非侵入式修改下其实不需要
                    //     CtClass cls = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    //     CtMethod mtd = cls.getDeclaredMethod("shouldPad");
                    //     mtd.setBody("{$0.shouldPad = false;}");
                    //     return cls.toBytecode();
                } else if (className.equals("common/Authorization")) {
                    // 设置破解key
                    CtClass cls = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    String func = "public static byte[] hex2bytes(String s) {" +
                            "   int len = s.length();" +
                            "   byte[] data = new byte[len / 2];" +
                            "   for (int i = 0; i < len; i += 2) {" +
                            "       data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));" +
                            "   }" +
                            "   return data;" +
                            "}";
                    CtMethod hex2bytes = CtNewMethod.make(func, cls);
                    cls.addMethod(hex2bytes);

                    CtConstructor mtd = cls.getDeclaredConstructor(new CtClass[]{});
                    mtd.setBody("{$0.watermark = 1234567890;" +
                            "$0.validto = \"forever\";" +
                            "$0.valid = true;" +
                            "common.MudgeSanity.systemDetail(\"valid to\", \"perpetual\");" +
                            "common.MudgeSanity.systemDetail(\"id\", String.valueOf($0.watermark));" +
                            "common.SleevedResource.Setup(hex2bytes(\"" + hexkey + "\"));" +
                            "}");
                    return cls.toBytecode();
                } else if (className.equals("aggressor/Aggressor") || className.equals("server/TeamServer") || className.equals("aggressor/headless/Start")) {
                    // Java 12无法修改final字段
                    // 解决方案 https://github.com/powermock/powermock/pull/1010/commits/66ce9f77215bae68b45f35481abc8b52a5d5b6ae
                    CtClass cls = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    CtMethod mfunc = cls.getDeclaredMethod("main");
                    classPool.importPackage("java.lang.reflect");
                    mfunc.insertBefore("" +
                            "Field field = aggressor.Aggressor.class.getDeclaredField(\"VERSION\");" +
                            "field.setAccessible(true);" +

                            // "Field modifiersField = Field.class.getDeclaredField(\"modifiers\");" +
                            "Field modifiersField = null;" +
                            "try {" +
                            "    modifiersField = Field.class.getDeclaredField(\"modifiers\");" +
                            "} catch (NoSuchFieldException e) {" +
                            "    try {" +
                            // https://www.javassist.org/tutorial/tutorial3.html#Varargs
                            // Varargs直接转换为数组
                            "        Method getDeclaredFields0 = Class.class.getDeclaredMethod(\"getDeclaredFields0\", new Class[]{boolean.class});" +
                            "        boolean accessibleBeforeSet = getDeclaredFields0.isAccessible();" +
                            "        getDeclaredFields0.setAccessible(true);" +
                            "        Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, new Object[]{Boolean.FALSE});" +
                            "        getDeclaredFields0.setAccessible(accessibleBeforeSet);" +
                            // javassist 不支持简写的for循环
                            "        for (int i=0; i<fields.length; i++){" +
                            "           Field field = fields[i];" +
                            "            if (\"modifiers\".equals(field.getName())) {" +
                            "                modifiersField = field;" +
                            "                break;" +
                            "            }" +
                            "        }" +
                            "        if (modifiersField == null) {" +
                            "            throw e;" +
                            "        }" +
                            "    } catch (NoSuchMethodException ex) {" +
                            "        e.addSuppressed(ex);" +
                            "        throw e;" +
                            "    } catch (InvocationTargetException ex) {" +
                            "        e.addSuppressed(ex);" +
                            "        throw e;" +
                            "    }" +
                            "}" +

                            "modifiersField.setAccessible(true);" +
                            "modifiersField.setInt(field, Modifier.STATIC);" +
                            // 奇怪的bug，如果写成field.getModifiers() & ~Modifier.FINAL 还是FINAL，改成STATIC就是STATIC
                            // 感觉是设置bit，而不是删除bit？
                            // "System.out.println(field.toString());" 可以输出modifier信息
                            // static java.lang.String aggressor.Aggressor.VERSION

                            "String fieldValue = (String) field.get(null);" +
                            // "System.out.println(fieldValue);" +
                            "field.set(null, fieldValue.replace(\")\", \"-Twi1ight@T00ls.Net)\"));"
                    );
                    return cls.toBytecode();
                } else if (className.equals("beacon/BeaconCommands")) {
                    if (Files.notExists(Paths.get("resources/bhelp.txt"))) {
                        return classfileBuffer;
                    }
                    System.out.println("[CSAgent] load translation resource");
                    CtClass cls = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    String[] funcs = {"loadCommands", "loadDetails"};
                    for (String func : funcs) {
                        CtMethod cm = cls.getDeclaredMethod(func);
                        cm.instrument(
                                new ExprEditor() {
                                    public void edit(MethodCall m)
                                            throws CannotCompileException {
                                        if (m.getClassName().equals("common.CommonUtils")
                                                && m.getMethodName().equals("bString")) {
                                            m.replace("{ $_ = new String($1, \"UTF-8\"); }");
                                        }
                                    }
                                });
                    }
                    return cls.toBytecode();
                } else if (className.equals("sleep/runtime/ScriptLoader")) {
                    // 解决Windows上中文乱码问题
                    if (Files.notExists(Paths.get("scripts/default.cna"))) {
                        return classfileBuffer;
                    }
                    CtClass cls = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    CtMethod mtd = cls.getDeclaredMethod("getInputStreamReader");
                    mtd.insertBefore("setCharset(\"UTF-8\");");
                    return cls.toBytecode();
                }
                if (needTranslation) {
                    // JButton的父类是AbstractButton，需要setActionCommand，否则翻译后无法正确执行Button的操作
                    if (className.equals("javax/swing/AbstractButton")) {
                        CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                        CtMethod ctMethod = ctClass.getDeclaredMethod("setText");
                        insertTranslateCommand(ctMethod, 1, false);
                        ctMethod.insertBefore("{setActionCommand($1);}");
                        // ctMethod.insertBefore("System.out.printf(\"AbstractButton.setText: %s\\n\", new Object[]{$1});");
                        return ctClass.toBytecode();
                    }
                    if (className.equals("javax/swing/JLabel")) {
                        CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                        CtMethod ctMethod = ctClass.getDeclaredMethod("setText");
                        insertTranslateCommand(ctMethod, 1, false);
                        // ctMethod.insertBefore("System.out.printf(\"JLabel.setText: %s\\n\", new Object[]{$1});");
                        return ctClass.toBytecode();
                    }
                    if (className.equals("javax/swing/JComponent")) {
                        CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                        CtMethod ctMethod = ctClass.getDeclaredMethod("setToolTipText");
                        insertTranslateCommand(ctMethod, 1, false);
                        // ctMethod.insertBefore("System.out.printf(\"JComponent.setToolTipText: %s\\n\", new Object[]{$1});");
                        return ctClass.toBytecode();
                    }
                    // JOptionPane.showMessageDialog 最终都会调用showOptionDialog
                    // 大部分消息都是带参数的，所以需要使用正则匹配
                    if (className.equals("javax/swing/JOptionPane")) {
                        CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                        //showOptionDialog 参数3是Title可被JDialog覆盖，参数2是Message，常用于提示信息，需正则替换
                        CtMethod ctMethod = ctClass.getDeclaredMethod("showOptionDialog",
                                new CtClass[]{
                                        classPool.get("java.awt.Component"),
                                        classPool.get("java.lang.Object"),
                                        classPool.get("java.lang.String"),
                                        CtClass.intType,
                                        CtClass.intType,
                                        classPool.get("javax.swing.Icon"),
                                        classPool.get("java.lang.Object[]"),
                                        classPool.get("java.lang.Object"),
                                });
                        insertTranslateCommand(ctMethod, 2, true);
                        // ctMethod.insertBefore("System.out.printf(\"JOptionPane.showOptionDialog2: %s\\n\", new Object[]{$2});");

                        //showInputDialog 参数3是Title可被JDialog覆盖，参数2是Message，暂无正则需求
                        // ctMethod = ctClass.getDeclaredMethod("showInputDialog",
                        //         new CtClass[]{
                        //                 classPool.get("java.awt.Component"),
                        //                 classPool.get("java.lang.Object"),
                        //                 classPool.get("java.lang.String"),
                        //                 CtClass.intType,
                        //                 classPool.get("javax.swing.Icon"),
                        //                 classPool.get("java.lang.Object[]"),
                        //                 classPool.get("java.lang.Object"),
                        //         });
                        // insertTranslateCommand((CtBehavior) ctMethod, 2, false);
                        // ctMethod.insertBefore("System.out.printf(\"JOptionPane.showInputDialog: %s\\n\", new Object[]{$2});");
                        return ctClass.toBytecode();
                    }
                    // 各类文件打开窗口标题
                    if (className.equals("javax/swing/JDialog")) {
                        CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                        CtConstructor ctConstructor = ctClass.getDeclaredConstructor(new CtClass[]{classPool
                                .get("java.awt.Frame"), classPool.get("java.lang.String"), CtClass.booleanType});
                        insertTranslateCommand(ctConstructor, 2, false);
                        // ctConstructor.insertBefore("System.out.printf(\"JDialog.setText: %s\\n\", new Object[]{$2});");
                        return ctClass.toBytecode();
                    }
                    // About等窗口标题
                    if (className.equals("javax/swing/JFrame")) {
                        CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                        CtConstructor ctConstructor = ctClass.getDeclaredConstructor(new CtClass[]{classPool.get("java.lang.String")});
                        insertTranslateCommand(ctConstructor, 1, false);
                        // ctConstructor.insertBefore("System.out.printf(\"JFrame: %s\\n\", new Object[]{$1});");
                        return ctClass.toBytecode();
                    }
                    // 各个Dialog上面的描述
                    if (className.equals("javax/swing/JEditorPane")) {
                        CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                        CtMethod ctMethod = ctClass.getDeclaredMethod("setText");
                        insertTranslateCommand(ctMethod, 1, false);
                        // ctMethod.insertBefore("System.out.printf(\"JEditorPane.setText: %s\\n\", new Object[]{$1});");
                        return ctClass.toBytecode();
                    }
                    // 最终会调用JLabel
                    // if (className.equals("javax/swing/JTabbedPane")) {
                    //     CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    //     CtMethod ctMethod = ctClass.getDeclaredMethod("addTab");
                    //     insertTranslateCommand((CtBehavior) ctMethod, 1);
                    //     ctMethod.insertBefore("System.out.printf(\"JTabbedPane.addTab: %s\\n\", new Object[]{$1});");
                    //     ctMethod = ctClass.getDeclaredMethod("insertTab");
                    //     insertTranslateCommand((CtBehavior) ctMethod, 1, false);
                    //     ctMethod.insertBefore("System.out.printf(\"JTabbedPane.insertTab: %s\\n\", new Object[]{$1});");
                    //     return ctClass.toBytecode();
                    // }
                    // 只在About和System Information里面有用到
                    // if (className.equals("javax/swing/JTextArea")) {
                    //     CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    //     CtMethod ctMethod = ctClass.getDeclaredMethod("setText");
                    //     insertTranslateCommand((CtBehavior) ctMethod, 1, false);
                    //     ctMethod.insertBefore("System.out.printf(\"JTextArea.setText: %s\\n\", new Object[]{$1});");
                    //     return ctClass.toBytecode();
                    // }
                    // 未找到调用点
                    // if (className.equals("java/awt/Frame") || className.equals("java/awt/Dialog")) {
                    //     CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    //     CtMethod ctMethod = ctClass.getDeclaredMethod("setTitle");
                    //     insertTranslateCommand((CtBehavior) ctMethod, 1, false);
                    //     ctMethod.insertBefore("System.out.printf(\"Frame|Dialog.setTitle: %s\\n\", new Object[]{$1});");
                    //     return ctClass.toBytecode();
                    // }
                    // 未找到调用点
                    // if (className.equals("javax/swing/text/JTextComponent)")) {
                    //     CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    //     CtMethod ctMethod = ctClass.getDeclaredMethod("setText");
                    //     insertTranslateCommand((CtBehavior) ctMethod, 1, false);
                    //     ctMethod.insertBefore("System.out.printf(\"JTextComponent.setText: %s\\n\", new Object[]{$1});");
                    //     return ctClass.toBytecode();
                    // }
                    // 未找到调用点
                    // if (className.equals("javax/swing/JComboBox")) {
                    //     CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    //     CtMethod ctMethod = ctClass.getDeclaredMethod("addItem");
                    //     insertTranslateCommand((CtBehavior) ctMethod, 1, false);
                    //     ctMethod.insertBefore("System.out.printf(\"JComboBox.addItem: %s\\n\", new Object[]{$1});");
                    //     return ctClass.toBytecode();
                    // }
                    // Console交互窗口中的文字信息
                    // if (className.equals("javax/swing/text/AbstractDocument")) {
                    //     CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    //     CtMethod ctMethod = ctClass.getDeclaredMethod("insertString");
                    //     insertTranslateCommand((CtBehavior) ctMethod, 2, false);
                    //     ctMethod.insertBefore("System.out.printf(\"AbstractDocument.insertString: %s\\n\", new Object[]{$2});");
                    //     return ctClass.toBytecode();
                    // }
                }
            } catch (Exception ex) {
                System.out.printf("[CSAgent] PreMain transform fucked up: %s\n", ex);
            }
            return classfileBuffer;
        }

        CtBehavior insertTranslateCommand(CtBehavior ctMethod, int n, Boolean regex) throws CannotCompileException {
            StringBuilder stringBuffer = new StringBuilder();
            stringBuffer.append("{");
            stringBuffer.append("ClassLoader classLoader = ClassLoader.getSystemClassLoader();");
            stringBuffer.append("Class translator = classLoader.loadClass(\"com.fuckme.Translator\");");
            if (regex) {
                stringBuffer.append("java.lang.reflect.Method method = translator.getDeclaredMethod(\"regexTranslate\",new Class[]{String.class});");
            } else {
                stringBuffer.append("java.lang.reflect.Method method = translator.getDeclaredMethod(\"translate\",new Class[]{String.class});");
            }
            stringBuffer.append(String.format("if($%d instanceof String){$%d = (String)method.invoke(null, new Object[]{$%d});}", n, n, n));
            stringBuffer.append("}");
            StringBuilder outer = new StringBuilder();
            outer.append("if ((javax.swing.table.DefaultTableCellRenderer.class.isAssignableFrom($0.getClass())  && !sun.swing.table.DefaultTableCellHeaderRenderer.class.isAssignableFrom($0.getClass()))  || javax.swing.text.DefaultStyledDocument.class.isAssignableFrom($0.getClass())  || javax.swing.tree.DefaultTreeCellRenderer.class.isAssignableFrom($0.getClass())  || $0.getClass().getName().equals(\"javax.swing.plaf.synth.SynthComboBoxUI$SynthComboBoxRenderer\")) {} else");
            outer.append(stringBuffer);
            try {
                ctMethod.insertBefore(outer.toString());
            } catch (Exception e) {
                ctMethod.insertBefore(stringBuffer.toString());
            }
            return ctMethod;
        }
    }
}