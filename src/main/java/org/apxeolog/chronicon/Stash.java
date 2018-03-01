package org.apxeolog.chronicon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apxeolog.chronicon.util.ByteBufferUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author APXEOLOG (Artyom Melnikov), at 01.03.2018
 */
@Slf4j @Getter
public class Stash {

    private final String version;
    private final String slotsCount;
    private final List<Item> itemList;

    private byte[] header;

    @ToString
    public static class Item {

        private final int unknownField1;
        private final int unknownField2;
        private int outerValue;

        @Getter
        private final List<Attribute> attributeList;

        public Item(int unknownField1, int unknownField2) {
            this.unknownField1 = unknownField1;
            this.unknownField2 = unknownField2;
            this.attributeList = new ArrayList<>(40);
        }

        public static Item from(byte[] data) throws DecoderException, IOException {
            ByteBuffer itemData = ByteBuffer.wrap(Hex.decodeHex(new String(data))).order(ByteOrder.LITTLE_ENDIAN);
            Item item = new Item(itemData.getInt(), itemData.getInt());
            while (itemData.hasRemaining()) {
                int unknownAttrField = itemData.getInt();
                String attrName = ByteBufferUtils.getString(itemData);
                int attrType = itemData.getInt();
                Object attrValue = null;
                switch (attrType) {
                    case 0:
                        attrValue = itemData.getDouble();
                        break;
                    case 1:
                        attrValue = ByteBufferUtils.getString(itemData);
                        break;
                    default:
                        throw new IOException("Unknown attribute type: " + attrType);
                }
                Item.Attribute attribute = new Item.Attribute(unknownAttrField, attrName, attrValue);
                item.getAttributeList().add(attribute);
            }
            return item;
        }

        public byte[] getBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(4000).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(unknownField1);
            buffer.putInt(unknownField2);
            for (Attribute attribute : attributeList) {
                buffer.putInt(attribute.unknownField1);
                ByteBufferUtils.putString(buffer, attribute.name);
                if (attribute.value instanceof Double) {
                    buffer.putInt(0);
                    buffer.putDouble((Double) attribute.value);
                } else if (attribute.value instanceof String) {
                    buffer.putInt(1);
                    ByteBufferUtils.putString(buffer, (String) attribute.value);
                }
            }
            byte[] result = new byte[buffer.position()];
            System.arraycopy(buffer.array(), 0, result, 0, result.length);
            log.info("Final buffer size: {}", result.length);
            return Hex.encodeHexString(result).getBytes();
        }

        @AllArgsConstructor @ToString
        public static class Attribute {

            private final int unknownField1;

            @Getter @Setter
            public String name;

            @Getter @Setter
            public Object value;
        }

        public String getName() {
             return (String) attributeList.stream()
                     .filter(attribute -> "name".equals(attribute.name))
                     .map(Attribute::getValue)
                     .findFirst()
                     .orElse("<None>");
        }
    }

    static class FakeItem extends Item {

        public FakeItem() {
            super(-1, -1);
        }
    }

    public Stash(String version, String slotsCount) {
        this.version = version;
        this.slotsCount = slotsCount;
        this.itemList = new ArrayList<>(30);
    }

    public static Stash fromFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String stashHexData = reader.readLine();
            Stash stash = new Stash(reader.readLine(), reader.readLine());
            stash.parseStashData(stashHexData);
            return stash;
        }
    }

    public void toFile(File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(getHexData());
            writer.newLine();
            writer.write(version);
            writer.newLine();
            writer.write(slotsCount);
        }
    }

    private void parseStashData(String hexData) throws IOException {
        try {
            ByteBuffer stashData = ByteBuffer.wrap(Hex.decodeHex(hexData))
                    .order(ByteOrder.LITTLE_ENDIAN);
            header = ByteBufferUtils.getByteArray(stashData, 12);
            while (stashData.hasRemaining()) {
                int smth = stashData.getInt();
                int itemDataLength = stashData.getInt();
                if (itemDataLength > 0) {
                    Item item = Item.from(ByteBufferUtils.getByteArray(stashData, itemDataLength));
                    item.outerValue = smth;
                    itemList.add(item);
                } else {
                    Item item = new FakeItem();
                    item.outerValue = smth;
                    itemList.add(item);
                }
                stashData.position(stashData.position() + 12); // 12 bytes padding
            }
        } catch (DecoderException ex) {
            throw new IOException("Cannot decode stash data", ex);
        }
    }

    private String getHexData() {
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(header);
        for (Item item : itemList) {
            buffer.putInt(item.outerValue);
            if (item instanceof  FakeItem) {
                buffer.putInt(0);
            } else {
                byte[] data = item.getBytes();
                buffer.putInt(data.length);
                buffer.put(data);
            }
            buffer.put(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        }
        byte[] result = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, result, 0, result.length);
        return Hex.encodeHexString(result);
    }
}
