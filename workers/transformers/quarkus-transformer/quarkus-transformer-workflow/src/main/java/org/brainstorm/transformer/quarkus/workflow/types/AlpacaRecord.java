package org.brainstorm.transformer.quarkus.workflow.types;

/**
 * Represents a record in the Alpaca format
 */
public class AlpacaRecord {
    private String instruction;
    private String input;
    private String output;

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
