package com.moneromint.solo;

import uk.offtopica.monerorpc.daemon.BlockTemplate;

import java.util.Objects;

public class NewBlockTemplateEvent {
    private final BlockTemplate blockTemplate;

    public NewBlockTemplateEvent(BlockTemplate blockTemplate) {
        this.blockTemplate = blockTemplate;
    }

    public BlockTemplate getBlockTemplate() {
        return blockTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewBlockTemplateEvent that = (NewBlockTemplateEvent) o;
        return Objects.equals(blockTemplate, that.blockTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockTemplate);
    }
}
