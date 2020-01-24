package com.nelkinda.bugreports.jfx;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Document;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetElementsByTagNameNSTest {
    private static WebEngine engine;
    private final NodeListLength nodeListLength = new NodeListLength();

    @BeforeAll
    static void loadTestPage() throws InterruptedException {
        final String testPage = loadTestPageFile();
        Platform.startup(() -> {
        });
        final CompletionMonitor loading = new CompletionMonitor();
        Platform.runLater(() -> {
            final WebView webView = new WebView();
            engine = webView.getEngine();
            engine.getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED)
                    loading.setDone();
            });
            engine.loadContent(testPage, "application/xhtml+xml");
        });
        loading.waitForDone();
    }

    @NotNull
    private static String loadTestPageFile() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE html>\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\n" +
                "<head><title>Test Page</title></head>\n" +
                "<body><h1>Test Page</h1></body>\n" +
                "</html>";
    }

    @ParameterizedTest
    @EnumSource(TestCase.class)
    void getDocumentElement(final TestCase testCase) throws InterruptedException {
        testCase.runner.accept(() -> nodeListLength.getValues(engine.getDocument()));
        nodeListLength.verify(testCase.toString());
    }

    // Used in @EnumSource
    @SuppressWarnings("unused")
    enum TestCase {
        JFX(Platform::runLater),
        SWING(SwingUtilities::invokeLater),
        OTHER(CompletableFuture::runAsync),
        CURRENT(Runnable::run);

        final Consumer<Runnable> runner;

        TestCase(final Consumer<Runnable> runner) {
            this.runner = runner;
        }
    }

    static class CompletionMonitor {
        volatile boolean done;

        void setDone() {
            if (done) return;
            done = true;
            synchronized (this) {
                notifyAll();
            }
        }

        void waitForDone() throws InterruptedException {
            if (!done)
                synchronized (this) {
                    while (!done)
                        wait();
                }
        }
    }

    static class NodeListLength {
        static final String XHTML_NS_URI = "http://www.w3.org/1999/xhtml";
        private final CompletionMonitor monitor = new CompletionMonitor();
        int lengthOfNSNodeList;
        int lengthOfNodeList;

        void verify(final String message) throws InterruptedException {
            monitor.waitForDone();
            assertAll(
                    message,
                    () -> assertEquals(1, lengthOfNodeList, "Without Namespace"),
                    () -> assertEquals(1, lengthOfNSNodeList, "With Namespace")
            );
        }

        void getValues(final Document doc) {
            lengthOfNodeList = doc.getElementsByTagName("html").getLength();
            lengthOfNSNodeList = doc.getElementsByTagNameNS(XHTML_NS_URI, "html").getLength();
            monitor.setDone();
        }
    }
}
