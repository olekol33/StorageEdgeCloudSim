package edu.boun.edgecloudsim.task_generator;

import edu.boun.edgecloudsim.applications.sample_app5.MainApp;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdleActiveStorageLoadGeneratorTest {
    IdleActiveStorageLoadGenerator loadGenerator;

/*    @BeforeAll
    static void setupRedis(){
        MainApp.main(null);
        RedisListHandler.createList("CODING_PLACE");
    }*/

    @BeforeEach
    void setUp(){
        loadGenerator =new IdleActiveStorageLoadGenerator(100,20,
            "SINGLE_TIER","RANDOM_HOST");

    }
/*
    @AfterAll
    static void closeRedis(){
        RedisListHandler.closeConnection();
    }*/


    @Test
    void createParityTaskEmptyObject() {
        Task falseObjectTask = new Task(1,1,1,1,1,1,null,null,null);
        falseObjectTask.setObjectRead("falseObject");
        assertFalse(loadGenerator.createParityTask(falseObjectTask));
    }

/*    @Test
    void createParityTask() {
        Task task = new Task(1,1,1,1,1,1,null,null,null);
        task.setObjectRead("d1");
        assertTrue(loadGenerator.createParityTask(task));
    }*/
}