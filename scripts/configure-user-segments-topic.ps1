param(
    [string]$BootstrapServer = "kafka-1:9092",
    [string]$Topic = "user-segments",
    [int]$Partitions = 12,
    [int]$ReplicationFactor = 3
)

$ErrorActionPreference = "Stop"

docker exec kafka-1 /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server $BootstrapServer `
    --create `
    --if-not-exists `
    --topic $Topic `
    --partitions $Partitions `
    --replication-factor $ReplicationFactor `
    --config cleanup.policy=compact `
    --config min.cleanable.dirty.ratio=0.1 `
    --config segment.ms=600000

docker exec kafka-1 /opt/kafka/bin/kafka-configs.sh `
    --bootstrap-server $BootstrapServer `
    --entity-type topics `
    --entity-name $Topic `
    --alter `
    --add-config cleanup.policy=compact,min.cleanable.dirty.ratio=0.1,segment.ms=600000

docker exec kafka-1 /opt/kafka/bin/kafka-configs.sh `
    --bootstrap-server $BootstrapServer `
    --entity-type topics `
    --entity-name $Topic `
    --describe
