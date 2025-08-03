package nv.nadav.smart_home.repository;

import nv.nadav.smart_home.model.Device;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends MongoRepository<Device, ObjectId> {
    List<Device> findByRoom(String room);

    Optional<Device> findByDeviceId(String deviceId);

    @Query(value = "{}", fields = "{ 'deviceId': 1, '_id': 0 }")
    List<String> getDeviceIds();
}
