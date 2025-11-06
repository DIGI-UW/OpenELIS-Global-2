package org.ozeki.sms.dao;

import org.ozeki.sms.valueholder.OzekiMessageOut;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OzekiMessageOutDAO extends CrudRepository<OzekiMessageOut, Integer> {
}
