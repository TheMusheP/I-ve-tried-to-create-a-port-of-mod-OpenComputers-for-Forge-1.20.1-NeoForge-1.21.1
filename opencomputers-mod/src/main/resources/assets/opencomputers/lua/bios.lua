-- OpenComputers BIOS
-- Basic Input/Output System for computers

local component = component
local computer = computer

-- Initialize component registry
local eeprom = component.list("eeprom")()
local gpu = component.list("gpu")()
local screen = component.list("screen")()

-- Print function for debug output
local function print(...)
  local args = table.pack(...)
  for i = 1, args.n do
    if gpu and screen then
      -- TODO: Implement GPU text output
    end
  end
end

-- Boot from filesystem
local function tryBootFromFilesystem()
  local bootAddress = computer.getBootAddress()
  if bootAddress then
    local filesystem = component.proxy(bootAddress)
    if filesystem then
      if filesystem.exists("/init.lua") then
        local file = filesystem.open("/init.lua", "r")
        if file then
          local code = ""
          local chunk
          repeat
            chunk = filesystem.read(file, math.huge)
            code = code .. (chunk or "")
          until not chunk
          filesystem.close(file)
          
          if code then
            print("Booting from " .. bootAddress)
            local initFunction, err = load(code, "=/init.lua")
            if initFunction then
              initFunction()
            else
              print("Boot error: " .. (err or "unknown error"))
            end
          end
        end
      end
    end
  end
end

-- Main boot sequence
local function boot()
  print("OpenComputers BIOS")
  print("Boot sequence starting...")
  
  -- Try to boot from configured filesystem
  tryBootFromFilesystem()
  
  -- If no boot device, drop to emergency shell
  print("No bootable medium found")
  print("Computer halted")
  computer.shutdown()
end

-- Start boot process
boot()