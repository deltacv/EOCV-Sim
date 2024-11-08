/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.tuner;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel;
import com.github.serivesmejia.eocvsim.tuner.exception.CancelTunableFieldAddingException;
import com.github.serivesmejia.eocvsim.util.ReflectUtil;
import io.github.deltacv.eocvsim.virtualreflect.VirtualField;
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflectContext;
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection;
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection;
import org.jetbrains.annotations.Nullable;
import org.openftc.easyopencv.OpenCvPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("rawtypes")
public class TunerManager {

    Logger logger = LoggerFactory.getLogger(getClass());

    private final EOCVSim eocvSim;

    private final List<TunableField> fields = new ArrayList<>();

    private TunableFieldAcceptorManager acceptorManager = null;

    private static HashMap<Type, Class<? extends TunableField<?>>> tunableFieldsTypes = null;
    private static HashMap<Class<? extends TunableField<?>>, Class<? extends TunableFieldAcceptor>> tunableFieldAcceptors = null;

    private VirtualReflection reflect = JvmVirtualReflection.INSTANCE;

    private boolean firstInit = true;

    public TunerManager(EOCVSim eocvSim) {
        this.eocvSim = eocvSim;
    }

    public void init() {
        if(tunableFieldsTypes == null) {
            tunableFieldsTypes = new HashMap<>();
            // ...
            for(Class<? extends TunableField<?>> clazz : eocvSim.getClasspathScan().getScanResult().getTunableFieldClasses()) {
                tunableFieldsTypes.put(ReflectUtil.getTypeArgumentsFrom(clazz)[0], clazz);
            }
        }

        if(tunableFieldAcceptors == null) {
            tunableFieldAcceptors = new HashMap<>();
            // oh god...
            tunableFieldAcceptors.putAll(eocvSim.getClasspathScan().getScanResult().getTunableFieldAcceptorClasses());
        }

        // for some reason, acceptorManager becomes null after a certain time passes
        // (maybe garbage collected? i don't know for sure...), but we can simply recover
        // from this by creating a new one with the found acceptors by the scanner, no problem.
        if(acceptorManager == null)
            acceptorManager = new TunableFieldAcceptorManager(tunableFieldAcceptors);

        if (firstInit) {
            eocvSim.pipelineManager.onPipelineChange.doPersistent(this::reset);
            firstInit = false;
        }

        if (eocvSim.pipelineManager.getReflectTarget() != null) {
            addFieldsFrom(eocvSim.pipelineManager.getReflectTarget());
            eocvSim.visualizer.updateTunerFields(createTunableFieldPanels());

            for(TunableField field : fields.toArray(new TunableField[0])) {
                try {
                    field.init();
                } catch(CancelTunableFieldAddingException e) {
                    logger.info("Field " + field.getFieldName() + " was removed due to \"" + e.getMessage() + "\"");
                    fields.remove(field);
                }
            }
        }
    }

    public void update() {
        //update all fields
        for(TunableField field : fields.toArray(new TunableField[0])) {
            try {
                field.update();
            } catch(Exception ex) {
                logger.error("Error while updating field " + field.getFieldName(), ex);
            }

            //check if this field has requested to reevaluate config for all panels
            if(field.fieldPanel.hasRequestedAllConfigReeval()) {
                //if so, iterate through all fields to reevaluate
                for(TunableField f : fields.toArray(new TunableField[0])) {
                    f.fieldPanel.panelOptions.reevaluateConfig();
                }
            }
        }
    }

    public void reset() {
        fields.clear();
        init();
    }

    public Class<? extends TunableField> getTunableFieldOf(VirtualField field) {
        //we only accept non-final fields
        if (field.isFinal()) return null;

        Class<?> type = field.getType();
        if (field.getType().isPrimitive()) { //wrap to java object equivalent if field type is primitive
            type = ReflectUtil.wrap(type);
        }

        Class<? extends TunableField> tunableFieldClass = null;

        if(tunableFieldsTypes.containsKey(type)) {
            tunableFieldClass = tunableFieldsTypes.get(type);
        } else {
            //if we don't have a class yet, use our acceptors
            if(acceptorManager != null) tunableFieldClass = acceptorManager.accept(type);
            //still haven't got anything, give up here.
            if(tunableFieldClass == null) return null;
        }

        return tunableFieldClass;
    }

    public void addFieldsFrom(Object pipeline) {
        if (pipeline == null) return;

        VirtualReflectContext reflectContext = reflect.contextOf(pipeline);
        if(reflectContext == null) return;

        VirtualField[] fields = reflect.contextOf(pipeline).getFields();

        for (VirtualField field : fields) {
            Class<? extends TunableField> tunableFieldClass = getTunableFieldOf(field);

            // we can't handle this type
            if(tunableFieldClass == null) continue;

            //yay we have a registered TunableField which handles this.
            //now, lets do some more reflection to instantiate this TunableField
            //and add it to the list...
            try {
                Constructor<? extends TunableField> constructor = tunableFieldClass.getConstructor(Object.class, VirtualField.class, EOCVSim.class);
                this.fields.add(constructor.newInstance(pipeline, field, eocvSim));
            } catch(InvocationTargetException e) {
                if(e.getCause() instanceof CancelTunableFieldAddingException) {
                    String message = e.getCause().getMessage();
                    logger.info("Field " + field.getName() + " wasn't added due to \"" + message + "\"");
                } else {
                    logger.error("Reflection error while processing field: " + field.getName(), e.getCause());
                }
            } catch (Exception ex) {
                //oops rip
                logger.error("Reflection error while processing field: " + field.getName(), ex);
            }

        }
    }

    @Nullable public TunableField getTunableFieldWithLabel(String label) {
        TunableField labeledField = null;

        for(TunableField field : fields) {
            String fieldLabel = field.reflectionField.getLabel();

            if(fieldLabel != null && fieldLabel.equals(label)) {
                labeledField = field;
                break;
            }
        }

        if(labeledField != null) {
            labeledField.setIgnoreGuiUpdates(true);
        }
        return labeledField;
    }

    public void setVirtualReflection(VirtualReflection reflect) {
        this.reflect = reflect;
    }

    public void reevaluateConfigs() {
        for(TunableField field : fields) {
            field.fieldPanel.panelOptions.reevaluateConfig();
        }
    }

    private List<TunableFieldPanel> createTunableFieldPanels() {
        List<TunableFieldPanel> panels = new ArrayList<>();

        for (TunableField field : fields) {
            panels.add(new TunableFieldPanel(field, eocvSim));
        }

        return panels;
    }

}