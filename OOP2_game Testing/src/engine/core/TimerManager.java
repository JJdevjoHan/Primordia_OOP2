package engine.core;

import javax.management.timer.Timer;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class TimerManager {

    private final Map<Component, List<Timer>> pausedTimers = new HashMap<>();

    public void pauseTimers(Component comp) {

        List<Timer> stopped = new ArrayList<>();

        try {
            Class<?> cls = comp.getClass();

            while (cls != null) {

                for (Field field : cls.getDeclaredFields()) {

                    if (Timer.class.isAssignableFrom(field.getType())) {

                        field.setAccessible(true);

                        Timer timer = (Timer) field.get(comp);

                        if (timer != null && timer.isActive()) {
                            timer.stop();
                            stopped.add(timer);
                        }
                    }
                }

                cls = cls.getSuperclass();
            }

            pausedTimers.put(comp, stopped);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resumeTimers(Component comp) {

        List<Timer> timers = pausedTimers.remove(comp);

        if (timers == null) return;

        for (Timer timer : timers) {
            timer.start();
        }
    }
}