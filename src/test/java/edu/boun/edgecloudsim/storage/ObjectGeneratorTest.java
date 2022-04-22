package edu.boun.edgecloudsim.storage;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObjectGeneratorTest {
    //Tests difference between most and least common data objects in parities.
    //Expect diff of 1
    @Test
    void codingObjectOccurrence(){
        String policy = "CODING_PLACE";
        var objGen = new ObjectGenerator(policy);
        HashMap<String, List<Map>> listOfStripes = objGen.getListOfStripes();
        assertEquals(listOfStripes.size(),3);

    }
}