package com.foreverjava.Util.Hibernate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class Table1Service {

    @Autowired
    private Table1Repository table1Repository;

    public List<Table1> getAllRecords() {
        return table1Repository.findAll();
    }
}
