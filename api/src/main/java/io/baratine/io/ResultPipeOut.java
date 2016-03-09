/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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

package io.baratine.io;

import io.baratine.io.Pipes.PipeOutFlowImpl;

/**
 * {@code OutStream} is a source of a pipe to write.
 * <pre><code>
 *   service.publish(Pipes.out(new MyFlow()));
 * </code></pre>
 */
public interface ResultPipeOut<T>
{
  //
  // caller/publisher side
  //
  
  /**
   * Returns the publisher's flow callback.
   */
  default PipeOut.Flow<T> flow()
  {
    return new PipeOutFlowImpl<>(this);
  }
  
  /**
   * Lambda for an inline publisher.
   */
  void handle(PipeOut<T> pipe, Throwable exn);
  
  //
  // receiver/consumer side
  //

  /**
   * Service accepts the request.
   */
  default void ok(PipeIn<T> pipe)
  {
    throw new IllegalStateException(getClass().getName());
  }
  
  /**
   * Service rejects the request.
   */
  default void fail(Throwable exn)
  {
    throw new IllegalStateException(getClass().getName());
  }
}
