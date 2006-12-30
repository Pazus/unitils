package org.unitils.inject;

import junit.framework.TestCase;
import org.apache.commons.configuration.Configuration;
import org.unitils.core.ConfigurationLoader;
import org.unitils.inject.annotation.AutoInjectStatic;
import static org.unitils.inject.util.PropertyAccessType.FIELD;
import static org.unitils.inject.util.PropertyAccessType.SETTER;
import org.unitils.inject.util.Restore;

/**
 * Test for restoring values that where replaced during the static auto injection of the {@link InjectModule} after
 * a test was performed.
 *
 * @author Filip Neven
 * @author Tim Ducheyne
 */
@SuppressWarnings({"UnusedDeclaration"})
public class InjectModuleRestoreAutoInjectTest extends TestCase {

    /* Tested object */
    private InjectModule injectModule;


    /**
     * Initializes the test and test fixture.
     */
    protected void setUp() throws Exception {
        super.setUp();

        Configuration configuration = new ConfigurationLoader().loadConfiguration();
        injectModule = new InjectModule();
        injectModule.init(configuration);

        SetterInjectionTarget.stringProperty = "original value";
        FieldInjectionTarget.stringProperty = "original value";
        SetterInjectionTarget.primitiveProperty = 111;
        FieldInjectionTarget.primitiveProperty = 111;
    }


    /**
     * Tests the default restore (=old value).
     */
    public void testRestore_defaultRestore() {
        injectModule.injectObjects(new TestStaticSetterDefaultRestore());
        injectModule.restoreObjects();
        assertEquals("original value", SetterInjectionTarget.stringProperty);
        assertEquals("original value", FieldInjectionTarget.stringProperty);
    }


    /**
     * Tests the no restore.
     */
    public void testRestore_restoreNothing() {
        injectModule.injectObjects(new TestStaticSetterRestoreNothing());
        injectModule.restoreObjects();
        assertEquals("injected value", SetterInjectionTarget.stringProperty);
        assertEquals("injected value", FieldInjectionTarget.stringProperty);
    }


    /**
     * Tests restoring the old value.
     */
    public void testRestore_restoreOldValue() {
        injectModule.injectObjects(new TestStaticSetterRestoreOldValue());
        injectModule.restoreObjects();
        assertEquals("original value", SetterInjectionTarget.stringProperty);
        assertEquals("original value", FieldInjectionTarget.stringProperty);
    }

    /**
     * Tests restoring a null object value.
     */
    public void testRestore_restoreNull() {
        injectModule.injectObjects(new TestStaticSetterRestoreNull());
        injectModule.restoreObjects();
        assertNull(SetterInjectionTarget.stringProperty);
        assertNull(FieldInjectionTarget.stringProperty);
    }


    /**
     * Tests restoring a 0 primitive value.
     */
    public void testRestore_restore0() {
        injectModule.injectObjects(new TestStaticSetterRestore0());
        injectModule.restoreObjects();
        assertEquals(0, SetterInjectionTarget.primitiveProperty);
        assertEquals(0, FieldInjectionTarget.primitiveProperty);
    }


    /**
     * Test class containing a static setter and field injection and default restore (= old value).
     */
    private class TestStaticSetterDefaultRestore {

        @AutoInjectStatic(target = SetterInjectionTarget.class, propertyAccessType = SETTER)
        private String setterInject = "injected value";

        @AutoInjectStatic(target = FieldInjectionTarget.class, propertyAccessType = FIELD)
        private String fieldInject = "injected value";
    }

    /**
     * Test class containing a static setter and field injection and no restore.
     */
    private class TestStaticSetterRestoreNothing {

        @AutoInjectStatic(target = SetterInjectionTarget.class, restore = Restore.NO_RESTORE)
        private String setterInject = "injected value";

        @AutoInjectStatic(target = FieldInjectionTarget.class, propertyAccessType = FIELD, restore = Restore.NO_RESTORE)
        private String fieldInject = "injected value";
    }

    /**
     * Test class containing a static setter and field injection and old value restore.
     */
    private class TestStaticSetterRestoreOldValue {

        @AutoInjectStatic(target = SetterInjectionTarget.class, restore = Restore.OLD_VALUE)
        private String setterInject = "injected value";

        @AutoInjectStatic(target = FieldInjectionTarget.class, propertyAccessType = FIELD, restore = Restore.OLD_VALUE)
        private String fieldInject = "injected value";
    }

    /**
     * Test class containing a static setter and field injection and a null value restore.
     */
    private class TestStaticSetterRestoreNull {

        @AutoInjectStatic(target = SetterInjectionTarget.class, restore = Restore.NULL_OR_0_VALUE)
        private String setterInject = "injected value";

        @AutoInjectStatic(target = FieldInjectionTarget.class, propertyAccessType = FIELD, restore = Restore.NULL_OR_0_VALUE)
        private String fieldInject = "injected value";
    }


    /**
     * Test class containing a static setter and field injection and primitive 0 value restore.
     */
    private class TestStaticSetterRestore0 {

        @AutoInjectStatic(target = SetterInjectionTarget.class, restore = Restore.NULL_OR_0_VALUE)
        private int setterInject = 111;

        @AutoInjectStatic(target = FieldInjectionTarget.class, propertyAccessType = FIELD, restore = Restore.NULL_OR_0_VALUE)
        private int fieldInject = 111;
    }


    /**
     * Target for setter injection in the tests.
     */
    public static class SetterInjectionTarget {

        private static String stringProperty;

        private static int primitiveProperty;


        public static String getStringProperty() {
            return SetterInjectionTarget.stringProperty;
        }

        public static void setStringProperty(String stringProperty) {
            SetterInjectionTarget.stringProperty = stringProperty;
        }

        public static int getPrimitiveProperty() {
            return SetterInjectionTarget.primitiveProperty;
        }

        public static void setPrimitiveProperty(int primitiveProperty) {
            SetterInjectionTarget.primitiveProperty = primitiveProperty;
        }
    }

    /**
     * Target for field injection in the tests.
     */
    private static class FieldInjectionTarget {

        private static String stringProperty;

        private static int primitiveProperty;

    }
}
