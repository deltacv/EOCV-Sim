package org.firstinspires.ftc.robotcore.external;

import java.util.ArrayList;

public class Telemetry {

    private final ArrayList<ItemOrLine> telem = new ArrayList<>();
    private ArrayList<ItemOrLine> lastTelem = new ArrayList<>();

    public Item infoItem = new Item( "", "");
    public Item errItem = new Item("", "");

    private String captionValueSeparator = " : ";

    private volatile String lastTelemUpdate = "";
    private volatile String beforeTelemUpdate = "mai";

    private boolean autoClear = true;

    public synchronized Item addData(String caption, String value) {

        Item item = new Item(caption, value);
        item.valueSeparator = captionValueSeparator;

        telem.add(item);

        return item;

    }

    public synchronized Item addData(String caption, Func<?> valueProducer) {
        Item item = new Item(caption, valueProducer);
        item.valueSeparator = captionValueSeparator;

        telem.add(item);

        return item;
    }

    public synchronized Item addData(String caption, Object value) {
        Item item = new Item(caption, "");
        item.valueSeparator = captionValueSeparator;

        item.setValue(value);

        telem.add(item);

        return item;
    }

    public synchronized Item addData(String caption, String value, Object... args) {
        Item item = new Item(caption, "");
        item.valueSeparator = captionValueSeparator;

        item.setValue(value, args);

        telem.add(item);

        return item;
    }

    public synchronized Item addData(String caption, Func valueProducer, Object... args) {

        Item item = new Item(caption, "");
        item.valueSeparator = captionValueSeparator;

        item.setValue(valueProducer, args);

        telem.add(item);

        return item;

    }

    public synchronized Line addLine() {
        return addLine("");
    }

    public synchronized Line addLine(String caption) {
        Line line = new Line(caption);
        telem.add(line);
        return line;
    }

    @SuppressWarnings("unchecked")
    public synchronized void update() {
        lastTelemUpdate = "";
        lastTelem = (ArrayList<ItemOrLine>) telem.clone();

        evalLastTelem();

        if(autoClear) clear();
    }

    private synchronized void evalLastTelem() {
        StringBuilder inTelemUpdate = new StringBuilder();

        if (infoItem != null && !infoItem.caption.trim().equals("")) {
            inTelemUpdate.append(infoItem.toString()).append("\n");
        }

        if(lastTelem != null) {
            int i = 0;
            for (ItemOrLine iol : lastTelem) {
                if (iol instanceof Item) {
                    Item item = (Item) iol;
                    item.valueSeparator = captionValueSeparator;
                    inTelemUpdate.append(item.toString()); //to avoid volatile issues we write into a stringbuilder
                } else if (iol instanceof Line) {
                    Line line = (Line) iol;
                    inTelemUpdate.append(line.toString()); //to avoid volatile issues we write into a stringbuilder
                }

                if (i < lastTelem.size() - 1)
                    inTelemUpdate.append("\n"); //append new line if this is not the lastest item

                i++;
            }
        }

        if(errItem != null && !errItem.caption.trim().equals("")) {
            inTelemUpdate.append("\n").append(errItem.toString());
        }

        inTelemUpdate.append("\n<html></html>");

        lastTelemUpdate = inTelemUpdate.toString().trim(); //and then we write to the volatile, public one
    }

    public synchronized boolean removeItem(Item item) {
        if (telem.contains(item)) {
            telem.remove(item);
            return true;
        }

        return false;
    }

    public synchronized void clear() {
        for (ItemOrLine i : telem.toArray(new ItemOrLine[0])) {
            if (i instanceof Item) {
                if (!((Item) i).isRetained) telem.remove(i);
            } else {
                telem.remove(i);
            }
        }
    }

    public synchronized boolean hasChanged() {
        boolean hasChanged = !lastTelemUpdate.equals(beforeTelemUpdate);
        beforeTelemUpdate = lastTelemUpdate;

        return hasChanged;
    }

    public synchronized String getCaptionValueSeparator() {
        return captionValueSeparator;
    }

    public synchronized void setCaptionValueSeparator(String captionValueSeparator) {
        this.captionValueSeparator = captionValueSeparator;
    }

    public synchronized void setAutoClear(boolean autoClear) {
        this.autoClear = autoClear;
    }

    @Override
    public String toString() {
        evalLastTelem();
        return lastTelemUpdate;
    }

    private interface ItemOrLine {
        String getCaption();

        void setCaption(String caption);
    }

    public static class Item implements ItemOrLine {

        protected String caption = "";

        protected Func valueProducer = null;

        protected String valueSeparator = " : ";

        protected boolean isRetained = false;

        public Item(String caption, String value) {
            setCaption(caption);
            setValue(value);
        }

        public Item(String caption, Func valueProducer) {
            this.caption = caption;
            this.valueProducer = valueProducer;
        }

        public synchronized void setValue(String value) {
            setValue((Func<String>) () -> value);
        }

        public synchronized void setValue(Func func) {
            this.valueProducer = func;
        }

        public synchronized void setValue(Object value) {
            setValue(value.toString());
        }

        public synchronized void setValue(String value, Object... args) {
            setValue(String.format(value, args));
        }

        public synchronized void setValue(Func func, Object... args) {
            setValue((Func<String>) () -> String.format(func.value().toString(), args));
        }

        public synchronized String getCaption() {
            return caption;
        }

        public synchronized void setCaption(String caption) {
            this.caption = caption;
        }

        public synchronized boolean isRetained() {
            return isRetained;
        }

        public synchronized void setRetained(boolean retained) {
            this.isRetained = retained;
        }

        @Override
        public String toString() {
            return caption + " " + valueSeparator + " " + valueProducer.value().toString();
        }

    }

    public static class Line implements ItemOrLine {

        protected String caption;

        public Line(String caption) {
            this.caption = caption;
        }

        public synchronized String getCaption() {
            return caption;
        }

        public synchronized void setCaption(String caption) {
            this.caption = caption;
        }

        @Override
        public synchronized String toString() {
            return caption;
        }

    }

}
