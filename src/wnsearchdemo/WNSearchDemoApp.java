/*
 * WNSearchDemoApp.java
 */
package wnsearchdemo;

import java.io.FileInputStream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.dictionary.Dictionary;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;

/**
 * The main class of the application.
 */
public class WNSearchDemoApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override
    protected void startup() {
        show(new WNSearchDemoView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override
    protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of WNSearchDemoApp
     */
    public static WNSearchDemoApp getApplication() {
        return Application.getInstance(WNSearchDemoApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(WNSearchDemoApp.class, args);
        initLucene();
    }

    private static void initLucene() {
        try {

            //set default configuration for the application
            AppCode mainAppCode = GlobalResource.currentAppCode;
            mainAppCode.setConfigFilePath("D:\\file_properties.xml");
            mainAppCode.setTestFolder("D:\\50503372\\Tap Test");
            // init wordnet dictionary
            JWNL.initialize(new FileInputStream(GlobalResource.currentAppCode.getConfigFilePath()));
            if (GlobalResource.aDict == null) {
                GlobalResource.aDict = Dictionary.getInstance();
            }
            mainAppCode.setWSDMode(true);
            mainAppCode.parseTrecForRead();
        } catch (Exception ex) {
            JFrame Fr = new JFrame();
            JOptionPane p = new JOptionPane();
            p.showMessageDialog(Fr, "Please make a configuration for app first!");
        }
    }

    /**
     * A Task that loads the contents of a file into a String.
     */
    static class IndexFileTask extends Task<String, Void> {

        /**
         * Construct a LoadTextFileTask.
         *
         * @param file the file to load from.
         */
        IndexFileTask(Application application) {
            super(application);
        }

        /**
         * Load the file into a String and return it.  The
         * {@code progress} property is updated as the file is loaded.
         * <p>
         * If this task is cancelled before the entire file has been
         * read, null is returned.
         *
         * @return the contents of the {code file} as a String or null
         */
        @Override
        protected String doInBackground() {

            while (!isCancelled()) {
//                setProgress(contents.length(), 0, fileLength);
            }
//            if (!isCancelled()) {
//                return contents.toString();
//            } else {
//                return null;
//            }
            return null;
        }
    }
}


