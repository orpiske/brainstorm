# Transformers - Quarkus Workflow

Simple Quarkus-based workflow library to help designing pipelines interacting with 
LLM APIs.



```java
    final Steps.ChatMeta meta = Steps.ChatStep.using(chatModel)
        .withContext(c -> startChat(c, componentName, optionModel))
        .usingPrompt(this::generateQuestionPrompt).chat()
        .usingPrompt(this::generateAnswerPrompt).chat().andThen(c -> addRecords(c, alpacaRecords))
        .eval();
```