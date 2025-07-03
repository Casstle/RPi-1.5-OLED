module com.casstle.pi.oled_ssd1351 {
    requires org.slf4j;             //slf4j-api-2.0.0-alpha1.jar
    requires org.slf4j.simple;      //slf4j-simple-2.0.0-alpha1.jar & simplelogger.properties
    requires com.pi4j;
    requires com.pi4j.plugin.raspberrypi;
    requires com.pi4j.library.gpiod;
    requires com.pi4j.plugin.gpiod;
    requires com.pi4j.library.pigpio;
    requires com.pi4j.plugin.pigpio;
    requires com.pi4j.library.linuxfs;
    requires com.pi4j.plugin.linuxfs;
    requires java.desktop;

    uses com.pi4j.extension.Extension;
    uses com.pi4j.provider.Provider;

    // allow access to classes in the following namespaces for Pi4J annotation processing
    opens com.casstle.pi.oled to com.pi4j;

    exports com.casstle.pi.oled;
}