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

package io.baratine.web;

import java.lang.annotation.Annotation;

import io.baratine.inject.InjectionPoint;

public interface RouteBuilder
{
  RouteBuilder before(Class<? extends ServiceWeb> typeBefore);
  <X extends Annotation> RouteBuilder before(X ann, InjectionPoint<?> ip);
  
  OutBuilder to(ServiceWeb service);
  OutBuilder to(Class<? extends ServiceWeb> service);
  
  RouteBuilder after(Class<? extends ServiceWeb> typeAfter);
  <X extends Annotation> RouteBuilder after(X ann, InjectionPoint<?> ip);
}
