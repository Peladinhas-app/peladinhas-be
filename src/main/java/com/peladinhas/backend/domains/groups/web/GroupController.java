package com.peladinhas.backend.domains.groups.web;

import com.peladinhas.backend.auth.CurrentUserService;
import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupVisibility;
import com.peladinhas.backend.domains.groups.service.CreateGroupCommand;
import com.peladinhas.backend.domains.groups.service.GroupService;
import com.peladinhas.backend.shared.web.ApiEnumParser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final CurrentUserService currentUserService;
    private final GroupService groupService;

    public GroupController(
            final CurrentUserService currentUserService,
            final GroupService groupService) {
        this.currentUserService = currentUserService;
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(@Valid @RequestBody final CreateGroupRequest request) {
        GroupVisibility visibility = ApiEnumParser.parse(GroupVisibility.class, request.visibility(), "visibility");
        GroupEntity group = groupService.createGroup(new CreateGroupCommand(
                request.name(),
                request.description(),
                visibility,
                currentUserService.requireCurrentUserId()));
        return GroupResponse.from(group);
    }
}
