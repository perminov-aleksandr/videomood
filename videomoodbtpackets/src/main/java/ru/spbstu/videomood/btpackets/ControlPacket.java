package ru.spbstu.videomood.btpackets;

public class ControlPacket extends Packet {
    private Command command;

    private Object[] arguments;

    public ControlPacket(Command command, Object... arguments) {
        this.command = command;
        this.arguments = arguments;
    }

    public Command getCommand() {
        return command;
    }

    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public PacketType type() {
        return PacketType.DATA;
    }
}
