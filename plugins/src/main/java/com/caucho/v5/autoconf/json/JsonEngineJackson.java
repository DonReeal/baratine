/*
 * Copyright (c) 1998-2016 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.v5.autoconf.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.caucho.v5.json.JsonDeserializer;
import com.caucho.v5.json.JsonEngine;
import com.caucho.v5.json.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonEngineJackson implements JsonEngine
{
  private JsonSerializer _serializer = new JsonSerializerJackson();
  private JsonDeserializer _deserialzier = new JsonDeserializerJackson();

  private ObjectMapper _mapper = new ObjectMapper();

  public JsonSerializer getSerializer()
  {
    return _serializer;
  }

  public JsonDeserializer getDeserializer()
  {
    return _deserialzier;
  }

  class JsonSerializerJackson implements JsonSerializer {
    public void serialize(Writer writer, Object value)
      throws IOException
    {
      _mapper.writeValue(writer, value);
    }
  }

  class JsonDeserializerJackson implements JsonDeserializer {
    public <T> T deserialize(Reader reader, Class<T> cls)
      throws IOException
    {
      return _mapper.readValue(reader, cls);
    }
  }
}
