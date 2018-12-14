package com.boxboat.jenkins.test.library.trigger

import com.boxboat.jenkins.library.trigger.Trigger
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

import static org.junit.Assert.assertEquals

@RunWith(value = Parameterized.class)
class TriggerTest {

    @Parameter(value = 0)
    public List<Trigger> triggers

    @Parameter(value = 1)
    public List<Trigger> mergedTriggers

    @Test
    void testTriggerMerge() {
        assertEquals(Trigger.merge(triggers), mergedTriggers)
    }

    @Parameters(name = "{index}: {0}")
    static Collection<Object[]> data() {
        return [[
                        [
                                new Trigger(
                                        imagePaths: ["a", "b"],
                                        event: "test",
                                ),
                                new Trigger(
                                        imagePaths: ["b", "c"],
                                        event: "test",
                                ),
                                new Trigger(
                                        imagePaths: ["a", "b"],
                                        eventRegex: "test",
                                ),
                                new Trigger(
                                        imagePaths: ["a", "b"],
                                        eventRegex: "test",
                                        params: [[$class: 'StringParameterValue', name: 'a', value: 'b']]
                                ),
                        ],
                        [
                                new Trigger(
                                        imagePaths: ["a", "b", "c"],
                                        event: "test",
                                ),
                                new Trigger(
                                        imagePaths: ["a", "b"],
                                        eventRegex: "test",
                                ),
                                new Trigger(
                                        imagePaths: ["a", "b"],
                                        eventRegex: "test",
                                        params: [[$class: 'StringParameterValue', name: 'a', value: 'b']]
                                ),
                        ],
                ]]*.toArray()
    }

}
