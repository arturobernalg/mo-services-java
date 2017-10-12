/* ----------------------------------------------------------------------------
 * Copyright (C) 2013      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO Fixed Length Binary encoder
 * ----------------------------------------------------------------------------
 * Licensed under the European Space Agency Public License, Version 2.0
 * You may not use this file except in compliance with the License.
 *
 * Except as expressly set forth in this License, the Software is provided to
 * You on an "as is" basis and without warranties of any kind, including without
 * limitation merchantability, fitness for a particular purpose, absence of
 * defects or errors, accuracy or non-infringement of intellectual property rights.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * ----------------------------------------------------------------------------
 */
package esa.mo.mal.encoder.binary.variable;

/**
 * Implements the MALElementStreamFactory interface for a fixed length binary encoding.
 */
public class VariableBinaryStreamFactory extends esa.mo.mal.encoder.binary.base.BaseBinaryStreamFactory
{
  @Override
  public org.ccsds.moims.mo.mal.encoding.MALElementInputStream createInputStream(final byte[] bytes, final int offset)
  {
    return new VariableBinaryElementInputStream(bytes, offset);
  }

  @Override
  public org.ccsds.moims.mo.mal.encoding.MALElementInputStream createInputStream(final java.io.InputStream is)
          throws org.ccsds.moims.mo.mal.MALException
  {
    return new VariableBinaryElementInputStream(is);
  }

  @Override
  public org.ccsds.moims.mo.mal.encoding.MALElementOutputStream createOutputStream(final java.io.OutputStream os)
          throws org.ccsds.moims.mo.mal.MALException
  {
    return new VariableBinaryElementOutputStream(os);
  }
}
