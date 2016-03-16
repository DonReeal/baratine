/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.amp.vault;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.actor.MethodAmpBase;
import com.caucho.v5.amp.manager.ServiceConfig;
import com.caucho.v5.amp.proxy.ActorAmpBean;
import com.caucho.v5.amp.proxy.ActorAmpBeanBase;
import com.caucho.v5.amp.proxy.SkeletonClass;
import com.caucho.v5.amp.spi.ActorAmp;
import com.caucho.v5.amp.spi.ActorContainerAmp;
import com.caucho.v5.amp.spi.HeadersAmp;
import com.caucho.v5.amp.spi.MethodAmp;

import io.baratine.convert.Convert;
import io.baratine.inject.Key;
import io.baratine.service.Id;
import io.baratine.service.AssetId;
import io.baratine.service.OnLookup;
import io.baratine.service.Result;
import io.baratine.stream.ResultStream;

/**
 * Actor skeleton for a resource.
 */
public class SkeletonDataStore extends SkeletonClass
{
  private static final Map<Class<?>,Convert<String,?>> _convertMap
    = new HashMap<>();
  
  private VaultConfig _configResource;
  private SkeletonDataItem _skelEntity;
  private String _address;

  public SkeletonDataStore(ServiceManagerAmp ampManager,
                          Class<?> api,
                          ServiceConfig configService,
                          VaultConfig configResource)
  {
    super(ampManager, api, configService);

    _configResource = configResource;

    _address = configService.address();

    Class<?> entityClass = _configResource.entityType();

    _skelEntity = new SkeletonDataItem(ampManager,
                                     entityClass,
                                     configService,
                                     configResource);
    _skelEntity.introspect();
  }

  @Override
  public void introspect()
  {
    super.introspect();

    introspectOnLookup();
  }

  private void introspectOnLookup()
  {
    if (isImplemented(OnLookup.class)) {
      return;
    }

    Class<?> entityClass = _configResource.entityType();
    Key<Object> key = (Key) Key.of(entityClass);

    Provider<Object> provider = ampManager().inject().provider(key);
    MethodHandle setter = findIdSetter();
    Convert<String,?> converter = findConverter();

    MethodAmp onLookup = new MethodOnLookup(_skelEntity, provider, converter, setter);

    onLookup(onLookup);
  }

  private MethodHandle findIdSetter()
  {
    MethodHandle setter = null;

    Field idField = findIdField(_configResource.entityType());

    if (idField != null) {
      try {
        idField.setAccessible(true);

        setter = MethodHandles.lookup().unreflectSetter(idField);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    return setter;
  }

  private Convert<String,?> findConverter()
  {
    Field idField = findIdField(_configResource.entityType());
    
    if (idField != null) {
      Class<?> type = idField.getType();

      Convert<String,?> convert = _convertMap.get(type);
      
      if (convert != null) {
        return convert;
      }
      /*
      if (type.isAssignableFrom(String.class)) {
        return ConverterIdentity.CONVERTER;
      }
      */
      
      return ConverterIdentity.CONVERTER;
    }
    else {
      return ConverterIdentity.CONVERTER;
    }
  }

  /**
   * handles findOneBy methods
   *
   * @return
   */
  protected MethodAmp createFindOneMethod(Method method)
  {
    String field = method.getName().substring("findOneBy".length());

    field = field.toLowerCase();

    MethodFindOne ampMethod = new MethodFindOne(ampManager(),
                                                _skelEntity,
                                                _configResource.driver(),
                                                _address,
                                                new String[]{field});

    return ampMethod;
  }

  private Field findIdField(Class<?> entityClass)
  {
    if (entityClass == null) {
      return null;
    }

    Field idField = null;

    for (Field field : entityClass.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      if (field.isAnnotationPresent(Id.class)) {
        return field;
      }

      if (field.getName().equals("id") || field.getName().equals("_id")) {
        idField = field;
      }
    }

    Field idParent = findIdField(entityClass.getSuperclass());

    if (idParent != null && idParent.isAnnotationPresent(Id.class)) {
      return idParent;
    }
    else if (idField != null) {
      return idField;
    }
    else {
      return idParent;
    }
  }

  private static class MethodFindOne extends MethodAmpBase
  {
    private ServiceManagerAmp _ampManager;
    private SkeletonClass _skelEntity;

    private VaultDriver _driver;
    private String _address;
    private String[] _fields;

    MethodFindOne(ServiceManagerAmp ampManager,
                  SkeletonClass skel,
                  VaultDriver driver,
                  String address,
                  String[] fields)
    {
      _ampManager = ampManager;
      _skelEntity = skel;
      _driver = driver;
      _address = address;
      _fields = fields;
    }

    @Override
    public void query(HeadersAmp headers,
                      Result<?> result,
                      ActorAmp actor,
                      Object[] args)
    {
      _driver.findOne(_fields,
                      args,
                      result.of((id, r) -> getEntity(id, r)));
    }

    public void getEntity(Object id, Result result)
    {
      if (id == null) {
        result.ok(null);
      }
      else {
        Object obj = _ampManager.service(_address + '/' + id)
                                .as(_skelEntity.getApiClass());

        result.ok(obj);
      }
    }
  }

  private static class MethodOnLookup extends MethodAmpBase
  {
    private SkeletonClass _skelEntity;
    private Provider<Object> _provider;
    private MethodHandle _fieldSetter;
    private Convert<String,?> _converter;

    MethodOnLookup(SkeletonClass skel,
                   Provider<Object> provider,
                   Convert<String,?> converter,
                   MethodHandle fieldSetter)
    {
      _skelEntity = skel;
      _provider = provider;
      _converter = converter;
      _fieldSetter = fieldSetter;
    }

    @Override
    public void query(HeadersAmp headers,
                      Result<?> result,
                      ActorAmp actor,
                      Object arg1)
    {
      Object entity = _provider.get();

      String path = (String) arg1;

      if (path.startsWith("/")) {
        path = path.substring(1);
      }

      if (_fieldSetter != null) {
        try {
          _fieldSetter.invoke(entity, _converter.convert(path));
        } catch (Throwable e) {
          result.fail(e);
          return;
        }
      }

      ActorAmpBeanBase actorBean = (ActorAmpBeanBase) actor;
      ActorContainerAmp container = actorBean.getContainer();

      ActorAmp actorChild = new ActorAmpBean(_skelEntity,
                                             entity,
                                             null,
                                             container);

      ((Result) result).ok(actorChild);
    }
  }
  
  private static class ConverterIdentity implements Convert<String,String>
  {
    private static final Convert<String,String> CONVERTER
      = new ConverterIdentity();
    
    @Override
    public String convert(String source)
    {
      return source;
    }
    
  }
  
  private static class ConvertPathToLong implements Convert<String,Long>
  {
    private static final Convert<String,Long> CONVERTER
      = new ConvertPathToLong();
    
    @Override
    public Long convert(String source)
    {
      if (source == null || source.isEmpty()) {
        return new Long(0);
      }
      else if (source.length() != 11) {
        return Long.decode(source);
      }
      else {
        return AssetId.decode(source);
      }
    }
    
  }
  
  static {
    _convertMap.put(Object.class, ConverterIdentity.CONVERTER);
    _convertMap.put(String.class, ConverterIdentity.CONVERTER);
    
    _convertMap.put(Long.class, ConvertPathToLong.CONVERTER);
    _convertMap.put(long.class, ConvertPathToLong.CONVERTER);
  }
}
