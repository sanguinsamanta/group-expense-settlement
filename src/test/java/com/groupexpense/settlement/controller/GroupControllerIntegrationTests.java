package com.groupexpense.settlement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupexpense.settlement.dto.AddMemberRequest;
import com.groupexpense.settlement.dto.CreateGroupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/groups - creates a group with valid name and returns 201")
    void testCreateGroup_Success() throws Exception {
        CreateGroupRequest request = new CreateGroupRequest("Road Trip to Ladakh");

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Road Trip to Ladakh")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.members", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/groups - returns 400 when group name is blank or missing")
    void testCreateGroup_BlankName_ReturnsBadRequest() throws Exception {
        CreateGroupRequest blankRequest = new CreateGroupRequest("   ");

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", notNullValue()))
                .andExpect(jsonPath("$.validationErrors[0].field", is("name")));
    }

    @Test
    @DisplayName("GET /api/groups - lists all created groups")
    void testGetAllGroups_ReturnsList() throws Exception {
        mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateGroupRequest("Group Alpha"))));

        mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateGroupRequest("Group Beta"))));

        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].name", hasItems("Group Alpha", "Group Beta")));
    }

    @Test
    @DisplayName("GET /api/groups/{groupId} - retrieves group details and members")
    void testGetGroupById_Success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupRequest("Flat 303"))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Long groupId = ((Number) created.get("id")).longValue();

        mockMvc.perform(get("/api/groups/{groupId}", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(groupId.intValue())))
                .andExpect(jsonPath("$.name", is("Flat 303")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.members", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/groups/{groupId} - returns 404 for non-existent group")
    void testGetGroupById_NotFound() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("Group not found with id: 999999")));
    }

    @Test
    @DisplayName("POST /api/groups/{groupId}/members - adds a member and returns 201")
    void testAddMemberToGroup_Success() throws Exception {
        MvcResult groupResult = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupRequest("Trek Crew"))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> groupMap = objectMapper.readValue(groupResult.getResponse().getContentAsString(), Map.class);
        Long groupId = ((Number) groupMap.get("id")).longValue();

        AddMemberRequest memberRequest = new AddMemberRequest("Rahul");

        mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Rahul")))
                .andExpect(jsonPath("$.joinedAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/groups/{groupId}/members - returns 400 when member name is blank")
    void testAddMemberToGroup_BlankName_ReturnsBadRequest() throws Exception {
        MvcResult groupResult = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupRequest("Coding Team"))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> groupMap = objectMapper.readValue(groupResult.getResponse().getContentAsString(), Map.class);
        Long groupId = ((Number) groupMap.get("id")).longValue();

        AddMemberRequest blankRequest = new AddMemberRequest("   ");

        mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors[0].field", is("name")));
    }

    @Test
    @DisplayName("POST /api/groups/{groupId}/members - returns 404 when group is missing")
    void testAddMemberToGroup_MissingGroup_ReturnsNotFound() throws Exception {
        AddMemberRequest memberRequest = new AddMemberRequest("Priya");

        mockMvc.perform(post("/api/groups/{groupId}/members", 888888L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Group not found with id: 888888")));
    }

    @Test
    @DisplayName("GET /api/groups/{groupId}/members - returns members list and 404 if group missing")
    void testGetGroupMembers_SuccessAndNotFound() throws Exception {
        // Test missing group returns 404
        mockMvc.perform(get("/api/groups/{groupId}/members", 777777L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        // Create group and add two members
        MvcResult groupResult = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupRequest("Project Team"))))
                .andExpect(status().isCreated())
                .andReturn();

        Long groupId = ((Number) objectMapper.readValue(groupResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMemberRequest("Sam"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMemberRequest("Sara"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/groups/{groupId}/members", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Sam")))
                .andExpect(jsonPath("$[1].name", is("Sara")));
    }

    @Test
    @DisplayName("POST /api/groups/{groupId}/members - returns 409 Conflict when member is already in group")
    void testAddDuplicateMember_ReturnsConflict() throws Exception {
        MvcResult groupResult = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupRequest("Book Club"))))
                .andExpect(status().isCreated())
                .andReturn();

        Long groupId = ((Number) objectMapper.readValue(groupResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        // First addition succeeds
        mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMemberRequest("Aman"))))
                .andExpect(status().isCreated());

        // Second addition with same name to same group fails with 409 Conflict
        mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMemberRequest("Aman"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already a member of group")));
    }

    @Test
    @DisplayName("Should allow same member to join multiple distinct groups")
    void testAddSameMemberToMultipleGroups_Success() throws Exception {
        MvcResult g1Result = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupRequest("Gym Buddies"))))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult g2Result = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupRequest("Weekend Gamers"))))
                .andExpect(status().isCreated())
                .andReturn();

        Long g1Id = ((Number) objectMapper.readValue(g1Result.getResponse().getContentAsString(), Map.class).get("id")).longValue();
        Long g2Id = ((Number) objectMapper.readValue(g2Result.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        // Add "Dev" to Gym Buddies
        MvcResult m1 = mockMvc.perform(post("/api/groups/{groupId}/members", g1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMemberRequest("Dev"))))
                .andExpect(status().isCreated())
                .andReturn();

        Long devId1 = ((Number) objectMapper.readValue(m1.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        // Add "Dev" to Weekend Gamers (reuses Member entity)
        MvcResult m2 = mockMvc.perform(post("/api/groups/{groupId}/members", g2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMemberRequest("Dev"))))
                .andExpect(status().isCreated())
                .andReturn();

        Long devId2 = ((Number) objectMapper.readValue(m2.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        // Member ID is preserved across multiple groups
        org.junit.jupiter.api.Assertions.assertEquals(devId1, devId2);
    }
}
