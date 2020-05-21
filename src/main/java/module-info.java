open module com.moneromint.solo {
    // Netty uses sun.misc.Unsafe for some optional performance related hackery.
    requires jdk.unsupported;

    requires io.netty.buffer;
    requires io.netty.codec;
    requires io.netty.common;
    requires io.netty.handler;
    requires io.netty.transport;
    requires io.netty.transport.epoll;

    requires org.slf4j;

    requires com.fasterxml.jackson.databind;

    requires uk.offtopica.monerocore;
    requires uk.offtopica.monerorpc;
}
