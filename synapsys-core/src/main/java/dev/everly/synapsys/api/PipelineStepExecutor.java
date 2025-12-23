package dev.everly.synapsys.api;

public interface PipelineStepExecutor <INPUT, OUTPUT, REQUEST_CONTEXT>{
    OUTPUT executeStep(INPUT input, REQUEST_CONTEXT requestContext);
}
