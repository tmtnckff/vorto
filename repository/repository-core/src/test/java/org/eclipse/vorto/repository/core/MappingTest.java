/**
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.vorto.repository.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.eclipse.vorto.model.ModelId;
import org.eclipse.vorto.model.ModelType;
import org.eclipse.vorto.repository.AbstractIntegrationTest;
import org.eclipse.vorto.repository.domain.User;
import org.eclipse.vorto.repository.importer.Context;
import org.eclipse.vorto.repository.importer.FileUpload;
import org.eclipse.vorto.repository.importer.UploadModelResult;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class MappingTest extends AbstractIntegrationTest {


  @Test
  public void tesUploadMapping() throws IOException {
    UploadModelResult uploadResult =
        this.importer.upload(
            FileUpload.create("Color.type",
                IOUtils.toByteArray(
                    new ClassPathResource("sample_models/Color.type").getInputStream())), Context.create(createUserContext("admin", "playground"),Optional.empty()));
    assertEquals(true, uploadResult.isValid());
    assertNotNull(uploadResult.getHandleId());
    ModelInfo resource = uploadResult.getReports().get(0).getModel();
    assertEquals("org.eclipse.vorto.examples.type", resource.getId().getNamespace());
    assertEquals("Color", resource.getId().getName());
    assertEquals("1.0.0", resource.getId().getVersion());
    assertEquals(ModelType.Datatype, resource.getType());
    assertEquals(0, resource.getReferences().size());
    assertEquals("Color", resource.getDisplayName());
    assertNull(resource.getDescription());
    assertEquals(0, repositoryFactory.getRepository(createUserContext("admin", "playground")).search("*").size());
  }


  @Test
  public void testCheckinValidMapping() throws Exception {
    IUserContext userContext = createUserContext("admin", "playground"); 
    
    UploadModelResult uploadResult =
        this.importer.upload(
            FileUpload.create("Color.type",
                IOUtils.toByteArray(
                    new ClassPathResource("sample_models/Color.type").getInputStream())),Context.create(userContext,Optional.empty()));
    assertEquals(true, uploadResult.isValid());
    assertEquals(0, repositoryFactory.getRepository(userContext).search("*").size());

    User user = new User();
    user.setUsername("alex");

    Collection<User> users = new ArrayList<User>();
    users.add(user);

    when(userRepository.findAll()).thenReturn(users);

    this.importer.doImport(uploadResult.getHandleId(),Context.create(createUserContext("alex", "playground"),Optional.empty()));
    assertEquals(1, repositoryFactory.getRepository(userContext).search("*").size());

    uploadResult = this.importer.upload(
        FileUpload.create("sample.mapping",
            IOUtils.toByteArray(
                new ClassPathResource("sample_models/sample.mapping").getInputStream())),
        Context.create(createUserContext("admin", "playground"),Optional.empty()));
    assertEquals(true, uploadResult.getReports().get(0).isValid());
    this.importer.doImport(uploadResult.getHandleId(), Context.create(createUserContext("alex", "playground"),Optional.empty()));
    assertEquals(1, repositoryFactory.getRepository(userContext).search("Mapping").size());
  }

  @Test
  public void testGetMappingsOfEntityForTargetPlatform() throws Exception {
    importModel("Color.type");
    importModel("sample.mapping");
    Thread.sleep(2000);
    assertEquals(1,
        repositoryFactory.getRepository(createUserContext("admin", "playground"))
            .getMappingModelsForTargetPlatform(
                ModelId.fromReference("org.eclipse.vorto.examples.type.Color", "1.0.0"), "ios",Optional.empty())
            .size());
  }

  @Test
  public void testUsedByMappingOfEntity() throws Exception {
    importModel("Color.type");
    importModel("sample.mapping");
    Thread.sleep(2000);
    assertEquals(1,
        repositoryFactory.getRepository(createUserContext("admin", "playground"))
            .getByIdWithPlatformMappings(ModelId.fromReference("org.eclipse.vorto.examples.type.Color", "1.0.0"))
            .getReferencedBy().size());
    assertEquals("org.eclipse.vorto.examples.type:Color_ios:1.0.0",
        repositoryFactory.getRepository(createUserContext("admin", "playground"))
            .getByIdWithPlatformMappings(ModelId.fromReference("org.eclipse.vorto.examples.type.Color", "1.0.0"))
            .getReferencedBy().get(0).getPrettyFormat());

    assertEquals(1,
        repositoryFactory.getRepository(createUserContext("admin", "playground"))
            .getByIdWithPlatformMappings(ModelId.fromReference("org.eclipse.vorto.examples.type.Color", "1.0.0"))
            .getPlatformMappings().size());
  }

  @Test
  public void testGetPlatformMappingsOfEntity() throws Exception {
    importModel("Color.type");
    importModel("sample.mapping");
    Thread.sleep(2000);
    ModelInfo colorInfo = repositoryFactory.getRepository(createUserContext("admin", "playground"))
        .getByIdWithPlatformMappings(ModelId.fromReference("org.eclipse.vorto.examples.type.Color", "1.0.0"));
    assertEquals(1, colorInfo.getPlatformMappings().size());
    assertEquals("org.eclipse.vorto.examples.type:Color_ios:1.0.0", colorInfo.getPlatformMappings().keySet().iterator().next());
  }
}
