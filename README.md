# JFX Bug Review ID 9063423

When calling DOM API methods from a thread other than the JavaFX Application thread, the behavior is inconsistent.
Non-namespace methods behave as expected.
Namespace methods return wrong results.
