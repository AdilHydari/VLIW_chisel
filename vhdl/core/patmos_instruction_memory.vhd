library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity patmos_instruction_memory is
  port(
  		clk					  : in std_logic;
  	 	rst                   : in std_logic;
        address               : in unsigned(31 downto 0);
        inst_in               : in unsigned(31 downto 0); -- store
        inst_out              : out unsigned(31 downto 0); -- load
        read_enable           : in std_logic;
        write_enable          : in std_logic
  );
end entity patmos_instruction_memory;


architecture arch of patmos_instruction_memory is


  type instruction_memory is array (0 to 1024) of unsigned(31 downto 0);
  signal inst_mem : instruction_memory;

begin
 -- data_out <= (data_mem(to_integer(unsigned(address)) + 3) & 
 --              data_mem(to_integer(unsigned(address)) + 2) &
 --              data_mem(to_integer(unsigned(address)) + 1) &
 --              data_mem(to_integer(unsigned(address)))) when read_enable = '1';
  read: process(address, read_enable)
  begin
  	if (read_enable = '1') then
  		inst_out <= inst_mem(to_integer(unsigned(address)));
  	end if;
  end process;
  
  mem : process(clk, rst)
  begin
   -- if(rst = '1') then
    --    for i in 0 to 255 loop -- initialize register file
      --    data_mem(i)<= (others => '0');
       -- end loop;
    --els
  if (rising_edge(clk)) then
      if(write_enable = '1') then
       -- data_mem(to_integer(unsigned(address)) + 3) <= data_in(31 downto 24);
       -- data_mem(to_integer(unsigned(address)) + 2) <= data_in(23 downto 16);
       -- data_mem(to_integer(unsigned(address)) + 1) <= data_in(15 downto 8);
       -- data_mem(to_integer(unsigned(address))) <=     data_in(7 downto 0);
        inst_mem(to_integer(unsigned(address))) <= inst_in;
      end if;
    end if;
  end process mem;

end arch;  