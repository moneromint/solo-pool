package com.moneromint.solo;

import uk.offtopica.monerorpc.daemon.BlockTemplate;

public class NewBlockTemplateEvent {
    private final BlockTemplate blockTemplate;

    public NewBlockTemplateEvent(BlockTemplate blockTemplate) {
        this.blockTemplate = blockTemplate;
    }

    public BlockTemplate getBlockTemplate() {
        return blockTemplate;
    }
}
