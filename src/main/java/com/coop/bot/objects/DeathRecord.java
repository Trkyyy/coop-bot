    package com.coop.bot.objects;

    import net.minecraft.entity.EntityType;

    import java.time.Instant;
    import java.util.UUID;

    public class DeathRecord {
        private final UUID entityUUID;
        private final String entityName;
        private final EntityType<?> entityType;
        private final long timestamp;
        private final String dimension;
        private final String deathMessage;
        private final String damageSource;
        private final String killerName;
        private final UUID killerUUID;
        private final EntityType<?> killerType;
        private final double deathX;
        private final double deathY;
        private final double deathZ;

        /**
         * Builder
         */
        public static class Builder {

            // Fields
            private UUID entityUUID;
            private String entityName;
            private EntityType<?> entityType;
            private String dimension;
            private String killerName;
            private UUID killerUUID;
            private EntityType<?> killerType;
            private String deathMessage;
            private double deathX;
            private double deathY;
            private double deathZ;

            // Optional
            private String damageSource = "";

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

            public Builder entityType(EntityType<?> entityType) {
                this.entityType = entityType;
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

            public Builder deathLocation(double x, double y, double z, String dimension) {
                this.deathX = x;
                this.deathY = y;
                this.deathZ = z;
                this.dimension = dimension;
                return this;
            }

            public Builder deathLocation(double x, double y, double z) {
                this.deathX = x;
                this.deathY = y;
                this.deathZ = z;
                this.dimension = "Unknown Dimension";
                return this;
            }


            public DeathRecord build() {
                return new DeathRecord(this);
            }
        }

        private DeathRecord(Builder builder) {
            this.entityUUID = builder.entityUUID;
            this.entityName = builder.entityName;
            this.entityType = builder.entityType;
            this.timestamp = Instant.now().getEpochSecond();
            this.dimension = builder.dimension;
            this.deathMessage = builder.deathMessage;
            this.damageSource = builder.damageSource; // ,aybe drop this
            this.killerName = builder.killerName;
            this.killerUUID = builder.killerUUID;
            this.killerType = builder.killerType;
            this.deathX = builder.deathX;
            this.deathY = builder.deathY;
            this.deathZ = builder.deathZ;
        }

        // Getters
        public UUID getEntityUUID() { return entityUUID; }
        public String getEntityName() { return entityName; }
        public EntityType<?> getEntityType() { return entityType; }
        public long getTimestamp() { return timestamp; }
        public String getDimension() { return dimension; }
        public String getDeathMessage() { return deathMessage; }
        public String getDamageSource() { return damageSource; }
        public String getKillerName() { return killerName; }
        public UUID getKillerUUID() { return killerUUID; }
        public EntityType<?> getKillerType() { return killerType; }
        public double getDeathX() { return deathX; }
        public double getDeathY() { return deathY; }
        public double getDeathZ() { return deathZ; }

        public String getDeathLocation() {
            return String.format(" at [%.0f, %.0f, %.0f] in %s",
                    this.getDeathX(),
                    this.getDeathY(),
                    this.getDeathZ(),
                    this.getDimension());
        }

        @Override
        public String toString() {
            return "DeathRecord{" +
                    "entityUUID=" + entityUUID +
                    ", entityName='" + entityName + '\'' +
                    ", entityType=" + entityType +
                    ", timestamp=" + timestamp +
                    ", dimension='" + dimension + '\'' +
                    ", deathMessage='" + deathMessage + '\'' +
                    ", damageSource='" + damageSource + '\'' +
                    ", killerName='" + killerName + '\'' +
                    ", killerUUID=" + killerUUID +
                    ", killerType=" + killerType +
                    ", deathX=" + deathX +
                    ", deathY=" + deathY +
                    ", deathZ=" + deathZ +
                    '}';
        }
    }