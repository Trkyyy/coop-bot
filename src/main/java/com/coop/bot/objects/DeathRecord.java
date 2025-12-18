    package com.coop.bot.objects;

    import net.minecraft.entity.EntityType;

    import java.time.Instant;
    import java.util.UUID;

    public class DeathRecord {
        private final UUID entityUUID;
        private final String entityName;
        private final long timestamp;
        private final String dimension;
        private final String deathMessage;
        private final String damageSource;
        private final String killerName;
        private final UUID killerUUID;
        private final EntityType<?> killerType;
        private final int xpDropped;

        /**
         * Builder
         */
        public static class Builder {

            // Fields
            private UUID entityUUID;
            private String entityName;
            private String dimension;
            private String killerName;
            private UUID killerUUID;
            private EntityType<?> killerType;
            private String deathMessage;

            // Optional
            private String damageSource = "";
            private int xpDropped = 0;

            public Builder() {
            }

            public Builder entityUUID(UUID entityUUID) {
                this.entityUUID = entityUUID;
                return this;
            }

            public Builder entityName(String entityName) {
                this.entityName = entityName;
                return this;
            }

            public Builder killerName(String killerName) {
                this.killerName = killerName;
                return this;
            }

            public Builder killerUUID(UUID killerUUID) {
                this.killerUUID = killerUUID;
                return this;
            }

            public Builder killerType(EntityType<?> killerType) {
                this.killerType = killerType;
                return this;
            }

            public Builder deathMessage(String deathMessage) {
                this.deathMessage = deathMessage;
                return this;
            }

            public Builder damageSource(String damageSource) {
                this.damageSource = damageSource;
                return this;
            }

            public Builder xpDropped(int xpDropped) {
                this.xpDropped = xpDropped;
                return this;
            }


            public DeathRecord build() {
                return new DeathRecord(this);
            }
        }

        private DeathRecord(Builder builder) {
            this.entityUUID = builder.entityUUID;
            this.entityName = builder.entityName;
            this.timestamp = Instant.now().getEpochSecond();
            this.dimension = builder.dimension;
            this.deathMessage = builder.deathMessage;
            this.damageSource = builder.damageSource; // ,aybe drop this
            this.killerName = builder.killerName;
            this.killerUUID = builder.killerUUID;
            this.killerType = builder.killerType;
            this.xpDropped = builder.xpDropped;
        }

        // Getters
        public UUID getEntityUUID() { return entityUUID; }
        public String getEntityName() { return entityName; }
        public long getTimestamp() { return timestamp; }
        public String getDimension() { return dimension; }
        public String getDeathMessage() { return deathMessage; }
        public String getDamageSource() { return damageSource; }
        public String getKillerName() { return killerName; }
        public UUID getKillerUUID() { return killerUUID; }
        public EntityType<?> getKillerType() { return killerType; }
        public int getXpDropped() { return xpDropped; }

        @Override
        public String toString() {
            return "DeathRecord{" +
                    "entityUUID=" + entityUUID +
                    ", entityName='" + entityName + '\'' +
                    ", timestamp=" + timestamp +
                    ", dimension='" + dimension + '\'' +
                    ", deathMessage='" + deathMessage + '\'' +
                    ", damageSource='" + damageSource + '\'' +
                    ", killerName='" + killerName + '\'' +
                    ", killerUUID=" + killerUUID +
                    ", killerType=" + killerType +
                    ", xpDropped=" + xpDropped +
                    '}';
        }
    }