package persistence;

import java.util.List;
import java.util.Optional;
import pos.MenuItem;

public interface MenuRepository {

    List<MenuItem> findAll() throws Exception;

    Optional<MenuItem> findByName(String name) throws Exception;

    void save(MenuItem item) throws Exception;

    void delete(String name) throws Exception;
}
